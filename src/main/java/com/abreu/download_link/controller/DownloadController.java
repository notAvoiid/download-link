package com.abreu.download_link.controller;

import com.abreu.download_link.domain.YoutubeLinkRequest;
import com.abreu.download_link.service.YoutubeDownloadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DownloadController {

    private final YoutubeDownloadService downloadService;

    public DownloadController(YoutubeDownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @PostMapping("/download")
    public ResponseEntity<String> downloadAudio(@RequestBody YoutubeLinkRequest request) {
        try {
            String result = downloadService.downloadAudio(request.url());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}