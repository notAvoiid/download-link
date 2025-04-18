package com.abreu.download_link.service;

import com.abreu.download_link.domain.DownloadStatus;
import com.abreu.download_link.domain.ProcessResult;
import com.abreu.download_link.domain.YoutubeLinkRequest;
import com.abreu.download_link.domain.YoutubeResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class YoutubeDownloadService {

    private static final String DOWNLOAD_DIR = System.getProperty("user.dir") + "/downloads";
    private final Path downloadsDir = Paths.get(System.getProperty("user.dir"), "downloads").toAbsolutePath().normalize();

    private final YoutubeProcessManager processManager;
    private final FileSystemManager fileSystemManager;
    private final DownloadStatusManager statusManager;

    @PostConstruct
    public void init() throws IOException {
        fileSystemManager.createDirectoryWithPermissions(DOWNLOAD_DIR);
    }

    @Async
    public CompletableFuture<YoutubeResponse> downloadAudio(YoutubeLinkRequest request) {
        String url = request.url();

        try {
            statusManager.updateStatus(url, Status.STARTING, "Download starting");

            String expectedFileName = processManager.getExpectedFileName(url, DOWNLOAD_DIR);
            Path filePathMp3 = Paths.get(DOWNLOAD_DIR, new File(expectedFileName).getName().replace(".webm", ".mp3"));

            log.info("Checking file existence: {}", filePathMp3);

            if (Files.exists(filePathMp3)) {
                log.info("File already exists: {}", filePathMp3);
                this.clearDownloads();
                return CompletableFuture.completedFuture(new YoutubeResponse("File already exists", filePathMp3.toString(), Status.ALREADY_EXISTS));
            }

            statusManager.updateStatus(url, Status.IN_PROGRESS, "Download in progress");
            log.info("Starting download for URL: {}", url);

            ProcessResult result = processManager.executeDownload(url, DOWNLOAD_DIR);
            Path downloadedFilePath = Paths.get(DOWNLOAD_DIR, new File(result.filepath()).getName().replace(".webm", ".mp3"));

            if (result.exitCode() != 0 || !Files.exists(downloadedFilePath)) {
                statusManager.updateStatus(url, Status.FAILED, "Download Failed");
                throw new DownloadFailedException("Download failed. Exit code: " + result.exitCode());
            }

            statusManager.updateStatus(url, Status.COMPLETED, "Download completed successfully");
            log.info("Download completed: {}", downloadedFilePath);

            return CompletableFuture.completedFuture(new YoutubeResponse("Download completed successfully", downloadedFilePath.toString(), Status.COMPLETED));

        } catch (IOException e) {
            log.error("Error retrieving expected file name for URL {}: {}", url, e.getMessage(), e);
            throw new DownloadFailedException("Error retrieving expected file name: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Download was interrupted for URL {}: {}", url, e.getMessage(), e);
            throw new DownloadFailedException("Download was interrupted", e);
        }
    }

    public Resource getFile(String filename) {
        try {
            Path filePath = downloadsDir.resolve(filename).normalize();

            if (!filePath.startsWith(downloadsDir)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to the requested file is denied.");
            }

            Resource fileResource = new UrlResource(filePath.toUri());

            if (!fileResource.exists() || !fileResource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + filename);
            }

            return fileResource;

        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed URL.", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading file.", e);
        }
    }

    public void clearDownloads() {
        log.info("Clearing downloads directory");
        fileSystemManager.cleanDirectory(DOWNLOAD_DIR);
    }

    public DownloadStatus getStatus(String url) {
        return statusManager.getStatus(url);
    }

}
