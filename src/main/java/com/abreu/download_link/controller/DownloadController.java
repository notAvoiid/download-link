package com.abreu.download_link.controller;

import com.abreu.download_link.domain.YoutubeLinkRequest;
import com.abreu.download_link.domain.YoutubeResponse;
import com.abreu.download_link.domain.enums.Status;
import com.abreu.download_link.service.YoutubeDownloadService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class DownloadController {

    private final YoutubeDownloadService downloadService;

    public DownloadController(YoutubeDownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @PostMapping("/download")
    public CompletableFuture<ResponseEntity<YoutubeResponse>> downloadAudio(@RequestBody YoutubeLinkRequest request) {
        return downloadService.downloadAudio(request)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(System.getProperty("user.dir"), "downloads", filename);
            Resource fileResource = new UrlResource(filePath.toUri());

            if (!fileResource.exists() || !fileResource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(fileResource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status/{videoId}")
    public ResponseEntity<Status> getDownloadStatus(@PathVariable String videoId) {
        return ResponseEntity.ok(downloadService.getDownloadStatus(videoId));
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<String> clearDownloads() {
        boolean success = downloadService.clearDownloads();
        if (success) {
            return ResponseEntity.ok("Download folder cleared successfully.");
        } else {
            return ResponseEntity.internalServerError().body("Failed to clear download folder.");
        }
    }

}