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
        statusManager.updateStatus(url, Status.STARTING, "Download starting");

        try {
            String expectedFileName = processManager.getExpectedFileName(request.url(), DOWNLOAD_DIR);
            Path filePath = Paths.get(DOWNLOAD_DIR, new File(expectedFileName).getName());

            if (Files.exists(filePath)) {
                statusManager.updateStatus(url, Status.ALREADY_EXISTS, "File already exists");
                return CompletableFuture.completedFuture(new YoutubeResponse("File already exists", filePath.toString(), Status.ALREADY_EXISTS));
            }

            statusManager.updateStatus(url, Status.IN_PROGRESS, "Download in progress");
            ProcessResult result = processManager.executeDownload(url, DOWNLOAD_DIR);
            Path downloadedFilePath = Paths.get(DOWNLOAD_DIR, new File(result.filepath()).getName().replace(".webm", ".mp3"));

            if (result.exitCode() != 0 || !Files.exists(downloadedFilePath)) {
                throw new DownloadFailedException("Download failed. Exit code: " + result.exitCode());
            }

            statusManager.updateStatus(url, Status.COMPLETED, "Download completed successfully");
            return CompletableFuture.completedFuture(new YoutubeResponse("Download completed successfully", downloadedFilePath.toString(), Status.COMPLETED));

        } catch (IOException e) {
            log.error("IOException occurred while retrieving expected file name for URL {}: {}", url, e.getMessage());
            throw new DownloadFailedException("Error retrieving expected file name: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Download was interrupted for URL {}: {}", url, e.getMessage());
            throw new DownloadFailedException("Download was interrupted", e);
        }
    }

    public Resource getFile(String filename) {
        try {
            Path filePath = downloadsDir.resolve(filename).normalize();

            if (!filePath.startsWith(downloadsDir)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado ao arquivo solicitado.");
            }

            Resource fileResource = new UrlResource(filePath.toUri());

            if (!fileResource.exists() || !fileResource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado: " + filename);
            }

            return fileResource;

        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL malformada.", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao ler arquivo.", e);
        }
    }

    public boolean clearDownloads() {
        log.info("Clearing downloads directory");
        return fileSystemManager.cleanDirectory(DOWNLOAD_DIR);
    }

    public DownloadStatus getStatus(String url) {
        return statusManager.getStatus(url);
    }

}
