package com.abreu.download_link.controller;

import com.abreu.download_link.domain.DownloadStatus;
import com.abreu.download_link.domain.YoutubeLinkRequest;
import com.abreu.download_link.domain.YoutubeResponse;
import com.abreu.download_link.service.YoutubeDownloadService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class DownloadController {

    private final YoutubeDownloadService downloadService;

    public DownloadController(YoutubeDownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @PostMapping("/download")
    public CompletableFuture<ResponseEntity<YoutubeResponse>> downloadAudio(@RequestBody @Valid YoutubeLinkRequest request) {
        return downloadService.downloadAudio(request)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/status")
    public ResponseEntity<DownloadStatus> getStatus(@RequestParam String url) {
        return ResponseEntity.ok(downloadService.getStatus(url));
    }

    @GetMapping("download/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource fileResource = downloadService.getFile(filename);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(fileResource);
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<Void> clearDownloads() {
        downloadService.clearDownloads();
        return ResponseEntity.noContent().build();
    }

}