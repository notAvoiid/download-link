package com.abreu.download_link.service;

import com.abreu.download_link.domain.*;
import com.abreu.download_link.domain.enums.Status;
import com.abreu.download_link.exceptions.DownloadFailedException;
import com.abreu.download_link.exceptions.MalformedURLException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class YoutubeDownloadService {

    private String DOWNLOAD_DIR;
    private Path downloadsDir;
    private final ConcurrentHashMap<String, FileMetadata> fileAccessMap = new ConcurrentHashMap<>();

    private final YoutubeProcessManager processManager;
    private final FileSystemManager fileSystemManager;
    private final DownloadStatusManager statusManager;

    @PostConstruct
    public void init() throws IOException {
        try {
            Path tempDir = Files.createTempDirectory("yt-downloads-");
            DOWNLOAD_DIR = tempDir.toAbsolutePath().toString();
            downloadsDir = tempDir;

            log.info("""
                
                #######################################################
                Temporary download directory created:
                Path: {}
                Permissions: {}
                Is temporary: {}
                #######################################################""",
                    downloadsDir,
                    getDirectoryPermissions(downloadsDir),
                    Files.isDirectory(downloadsDir) && Files.isWritable(downloadsDir));

            fileSystemManager.createDirectoryWithPermissions(DOWNLOAD_DIR);
        } catch (IOException e) {
            log.error("""
                
                #######################################################
                FAILED TO CREATE TEMPORARY DIRECTORY!
                Error: {}
                Stack Trace: {}
                #######################################################""",
                    e.getMessage(),
                    Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }

    @Async
    public CompletableFuture<YoutubeResponse> downloadAudio(YoutubeLinkRequest request) {
        String url = request.url();
        Path filePath = null;

        try {
            statusManager.updateStatus(url, Status.STARTING, "Download starting");
            log.info("Starting download for: {}", url);

            // Obter nome do arquivo esperado
            String expectedFilePath = processManager.getExpectedFileName(url, DOWNLOAD_DIR);
            filePath = Paths.get(expectedFilePath);
            fileAccessMap.put(filePath.toString(), new FileMetadata(true));

            statusManager.updateStatus(url, Status.IN_PROGRESS, "Download in progress");
            log.info("Starting download for URL: {}", url);

            ProcessResult result = processManager.executeDownload(url, DOWNLOAD_DIR);

            if (result.exitCode() != 0 || !Files.exists(filePath)) {
                // Tentar encontrar o arquivo mais recente como fallback
                try (var files = Files.list(Paths.get(DOWNLOAD_DIR))) {
                    filePath = files
                            .filter(path -> path.toString().endsWith(".mp3"))
                            .max(Comparator.comparingLong(p -> {
                                try {
                                    return Files.getLastModifiedTime(p).toMillis();
                                } catch (IOException e) {
                                    return 0;
                                }
                            }))
                            .orElseThrow(() -> new DownloadFailedException("MP3 file not found after download"));
                }
            }

            fileAccessMap.put(filePath.toString(), new FileMetadata(true));

            if (!Files.exists(filePath)) {
                throw new DownloadFailedException("Download failed. File not created: " + filePath);
            }

            statusManager.updateStatus(url, Status.COMPLETED, "Download completed successfully");
            log.info("The file was downloaded at: {}", filePath);
            return CompletableFuture.completedFuture(
                    new YoutubeResponse("Download completed successfully", filePath.toString(), Status.COMPLETED)
            );

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Download failed for URL: {}", url, e);

            // Tentativa de recuperação: buscar qualquer arquivo MP3
            if (filePath == null || !Files.exists(filePath)) {
                try {
                    try (var files = Files.list(Paths.get(DOWNLOAD_DIR))) {
                        filePath = files
                                .filter(path -> path.toString().endsWith(".mp3"))
                                .findFirst()
                                .orElse(null);
                    }
                } catch (IOException ex) {
                    log.error("Error while trying to find downloaded file", ex);
                }
            }

            if (filePath != null && Files.exists(filePath)) {
                log.warn("Using fallback file: {}", filePath);
                return CompletableFuture.completedFuture(
                        new YoutubeResponse("Download completed with warnings", filePath.toString(), Status.COMPLETED)
                );
            }

            throw new DownloadFailedException("Download error", e);
        } finally {
            if (filePath != null && fileAccessMap.containsKey(filePath.toString())) {
                fileAccessMap.get(filePath.toString()).setInUse(false);
            }
        }
    }

    public Resource getFile(String filename) {
        try {
            Path filePath = downloadsDir.resolve(filename).normalize();
            if (!filePath.startsWith(downloadsDir)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }

            FileMetadata metadata = fileAccessMap.get(filePath.toString());
            if (metadata != null) {
                metadata.setInUse(false);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
            return resource;

        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URL");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading file");
        }
    }

    @Scheduled(fixedRate = 60 * 1000)
    public void scheduledCleanup() {
        log.info("Running scheduled cleanup...");
        Instant now = Instant.now();

        Set<String> filesToRemove = new HashSet<>();

        fileAccessMap.forEach((filePath, metadata) -> {
            if (!metadata.isInUse() && metadata.getLastUpdated().isBefore(now.minusSeconds(120))) {
                filesToRemove.add(filePath);
            }
        });

        filesToRemove.parallelStream().forEach(filePath -> {
            try {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    Files.delete(path);
                    log.info("File {} removed by scheduled cleanup", filePath);
                }
                fileAccessMap.remove(filePath);
            } catch (IOException e) {
                log.error("Error deleting {}: {}", filePath, e.getMessage());
            }
        });
    }


    public DownloadStatus getStatus(String url) {
        return statusManager.getStatus(url);
    }

    private String getDirectoryPermissions(Path path) {
        try {
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                return "Permissions: " + PosixFilePermissions.toString(Files.getPosixFilePermissions(path));
            } else {
                return "Basic permissions - Readable: " + Files.isReadable(path)
                        + ", Writable: " + Files.isWritable(path)
                        + ", Executable: " + Files.isExecutable(path);
            }
        } catch (IOException e) {
            return "Could not check permissions: " + e.getMessage();
        }
    }
}