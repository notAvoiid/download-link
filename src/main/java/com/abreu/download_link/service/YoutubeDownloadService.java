package com.abreu.download_link.service;

import com.abreu.download_link.domain.ProcessResult;
import com.abreu.download_link.domain.YoutubeLinkRequest;
import com.abreu.download_link.domain.YoutubeResponse;
import com.abreu.download_link.domain.enums.Status;
import com.abreu.download_link.exceptions.DownloadFailedException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

    private final YoutubeUrlValidator urlValidator;
    private final YoutubeProcessManager processManager;
    private final FileSystemManager fileSystemManager;
    private final DownloadStatusManager statusManager;

    @PostConstruct
    public void init() throws IOException {
        fileSystemManager.createDirectoryWithPermissions(DOWNLOAD_DIR);
    }

    @Async
    public CompletableFuture<YoutubeResponse> downloadAudio(YoutubeLinkRequest request) {
        final String videoId = urlValidator.validateAndExtractVideoId(request.url());
        statusManager.updateStatus(videoId, Status.STARTING);

        try {
            String expectedFileName = processManager.getExpectedFileName(request.url(), DOWNLOAD_DIR);
            Path filePath = Paths.get(DOWNLOAD_DIR, new File(expectedFileName).getName());

            if (Files.exists(filePath)) {
                statusManager.updateStatus(videoId, Status.ALREADY_EXISTS);
                return CompletableFuture.completedFuture(new YoutubeResponse("File already exists", filePath.toString(), Status.ALREADY_EXISTS));
            }

            statusManager.updateStatus(videoId, Status.IN_PROGRESS);
            ProcessResult result = processManager.executeDownload(request.url(), DOWNLOAD_DIR);
            Path downloadedFilePath = Paths.get(result.filepath());

            if (result.exitCode() != 0 || !Files.exists(downloadedFilePath)) {
                throw new DownloadFailedException("Download failed. Exit code: " + result.exitCode());
            }

            statusManager.updateStatus(videoId, Status.COMPLETED);
            return CompletableFuture.completedFuture(new YoutubeResponse("Download completed successfully", filePath.toString(), Status.COMPLETED));

        } catch (IOException e) {
            log.error("IOException occurred while retrieving expected file name for URL {}: {}", request.url(), e.getMessage());
            throw new DownloadFailedException("Error retrieving expected file name: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Download was interrupted for URL {}: {}", request.url(), e.getMessage());
            throw new DownloadFailedException("Download was interrupted", e);
        }
    }

    public boolean clearDownloads() {
        log.info("Clearing downloads directory");
        return fileSystemManager.cleanDirectory(DOWNLOAD_DIR);
    }

    public Status getDownloadStatus(String videoId) {
        return statusManager.getStatus(videoId);
    }
}
