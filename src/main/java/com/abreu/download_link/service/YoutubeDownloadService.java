package com.abreu.download_link.service;

import com.abreu.download_link.domain.ProcessResult;
import com.abreu.download_link.domain.YoutubeLinkRequest;
import com.abreu.download_link.domain.YoutubeResponse;
import com.abreu.download_link.domain.enums.Status;
import com.abreu.download_link.exceptions.DownloadFailedException;
import com.abreu.download_link.exceptions.FileAlreadyExistsException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
        return CompletableFuture.supplyAsync(() -> {
            final String videoId = urlValidator.validateAndExtractVideoId(request.url());
            statusManager.updateStatus(videoId, Status.STARTING);

            try {
                String expectedFileName = processManager.getExpectedFileName(request.url(), DOWNLOAD_DIR);
                Path filePath = Paths.get(DOWNLOAD_DIR, expectedFileName);

                if (Files.exists(filePath)) {
                    statusManager.updateStatus(videoId, Status.ALREADY_EXISTS);
                    return new YoutubeResponse("File already exists", filePath.toString(), Status.ALREADY_EXISTS);
                }

                statusManager.updateStatus(videoId, Status.IN_PROGRESS);
                ProcessResult result = processManager.executeDownload(request.url(), DOWNLOAD_DIR);

                if (result.exitCode() != 0) {
                    throw new DownloadFailedException("Download failed. Exit code: " + result.exitCode());
                }

                if (!Files.exists(filePath)) {
                    throw new DownloadFailedException("File overwritten " + filePath);
                }

                statusManager.updateStatus(videoId, Status.COMPLETED);
                return new YoutubeResponse("Download completed successfully", filePath.toString(), Status.COMPLETED);

            } catch (FileAlreadyExistsException e) {
                statusManager.updateStatus(videoId, Status.ALREADY_EXISTS);
                log.warn("File already exists: {}", e.getMessage());
                return new YoutubeResponse(e.getMessage(), null, Status.ALREADY_EXISTS);
            } catch (Exception e) {
                statusManager.updateStatus(videoId, Status.FAILED);
                log.error("Error downloading audio {}: {}", videoId, e.getMessage());
                throw new CompletionException(e);
            }
        });
    }

    public boolean clearDownloads() {
        return fileSystemManager.cleanDirectory(DOWNLOAD_DIR);
    }

    private String extractFilePath(String output) {
        return Arrays.stream(output.split("\n"))
                .filter(line -> line.contains("[ExtractAudio] Destination:"))
                .map(line -> line.replace("[ExtractAudio] Destination: ", "").trim())
                .findFirst()
                .orElseThrow(() -> new DownloadFailedException("File path not found in output"));
    }

    public Status getDownloadStatus(String videoId) {
        return statusManager.getStatus(videoId);
    }
}
