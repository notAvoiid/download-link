package com.abreu.download_link.controller;

import com.abreu.download_link.domain.YoutubeLinkRequest;
import com.abreu.download_link.service.YoutubeDownloadService;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class DownloadController {

    private final YoutubeDownloadService downloadService;

    public DownloadController(YoutubeDownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @Async
    @PostMapping("/download")
    public CompletableFuture<ResponseEntity<String>> downloadAudio(@RequestBody YoutubeLinkRequest request) {
        return downloadService.downloadAudio(request.url())
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> ResponseEntity.internalServerError().body("Error: " + e.getMessage()));
    }

}