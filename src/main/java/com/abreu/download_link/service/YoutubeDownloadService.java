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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Arrays;
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
        Path filePathWebm = null;
        Path filePathMp3 = null;

        try {
            statusManager.updateStatus(url, Status.STARTING, "Download starting");
            String expectedFileName = processManager.getExpectedFileName(url, DOWNLOAD_DIR);
            filePathWebm = Paths.get(DOWNLOAD_DIR, new File(expectedFileName).getName());
            fileAccessMap.put(filePathWebm.toString(), new FileMetadata(true));

            statusManager.updateStatus(url, Status.IN_PROGRESS, "Download in progress");
            log.info("Starting download for URL: {}", url);

            try {
                log.info("MP3 files at {}:", DOWNLOAD_DIR);

                Files.list(Paths.get(DOWNLOAD_DIR))
                        .filter(p -> p.toString().endsWith(".mp3"))
                        .forEach(mp3 -> {
                            try {
                                log.info("- {} ({} bytes, modified at {})",
                                        mp3.getFileName(),
                                        Files.size(mp3),
                                        Files.getLastModifiedTime(mp3));
                            } catch (IOException e) {
                                log.warn("- {} [details unavailable]", mp3.getFileName());
                            }
                        });
            } catch (IOException e) {
                log.error("Failed to list MP3s: {}", e.getMessage());
            }

            ProcessResult result = processManager.executeDownload(url, DOWNLOAD_DIR);
            log.info("Download directory: {}", DOWNLOAD_DIR);

            try (var files = Files.list(Paths.get(DOWNLOAD_DIR))) {
                filePathMp3 = files
                        .filter(path -> path.toString().endsWith(".mp3"))
                        .max((f1, f2) -> {
                            try {
                                return Files.getLastModifiedTime(f1).compareTo(Files.getLastModifiedTime(f2));
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .orElseThrow(() -> new DownloadFailedException("MP3 file not found after download"));
            }

            try {
                log.info("MP3 files in {}:", DOWNLOAD_DIR);

                Files.list(Paths.get(DOWNLOAD_DIR))
                        .filter(p -> p.toString().endsWith(".mp3"))
                        .forEach(mp3 -> {
                            try {
                                log.info("- {} ({} bytes, modified at {})",
                                        mp3.getFileName(),
                                        Files.size(mp3),
                                        Files.getLastModifiedTime(mp3));
                            } catch (IOException e) {
                                log.warn("- {} [details unavailable]", mp3.getFileName());
                            }
                        });
            } catch (IOException e) {
                log.error("Failed to list MP3s: {}", e.getMessage());
            }

            fileAccessMap.put(filePathMp3.toString(), new FileMetadata(true));

            if (result.exitCode() != 0 || !Files.exists(filePathMp3)) {
                throw new DownloadFailedException("Download failed. Exit code: " + result.exitCode());
            }

            statusManager.updateStatus(url, Status.COMPLETED, "Download completed successfully");
            log.info("The file was downloaded at: {}", filePathMp3);
            return CompletableFuture.completedFuture(
                    new YoutubeResponse("Download completed successfully", filePathMp3.toString(), Status.COMPLETED)
            );

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DownloadFailedException("Download error", e);
        } finally {
            if (filePathWebm != null && fileAccessMap.containsKey(filePathWebm.toString())) {
                fileAccessMap.get(filePathWebm.toString()).setInUse(false);
            }
            if (filePathMp3 != null && fileAccessMap.containsKey(filePathMp3.toString())) {
                fileAccessMap.get(filePathMp3.toString()).setInUse(false);
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