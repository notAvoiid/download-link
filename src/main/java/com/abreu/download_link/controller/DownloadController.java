package com.abreu.download_link.controller;

import com.abreu.download_link.domain.DownloadStatus;
import com.abreu.download_link.domain.YoutubeLinkRequest;
import com.abreu.download_link.domain.YoutubeResponse;
import com.abreu.download_link.service.YoutubeDownloadService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Tag(name = "YouTube Downloader", description = "APIs for downloading YouTube audio")
@Slf4j
public class DownloadController {

    private final YoutubeDownloadService downloadService;

    public DownloadController(YoutubeDownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @RequestMapping(value = "/download", method = {RequestMethod.POST, RequestMethod.GET})
    @Operation(
            summary = "Download audio from YouTube",
            description = "Submit a YouTube URL to start audio download",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Download initiated",
                            content = @Content(schema = @Schema(implementation = YoutubeResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid URL provided")
            }
    )
    public ResponseEntity<CompletableFuture<YoutubeResponse>> downloadAudio(
            @RequestBody @Valid YoutubeLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).
                body(downloadService.downloadAudio(request));
    }

    @GetMapping("/status")
    @Operation(
            summary = "Check download status",
            description = "Get the status of a YouTube audio download by URL"
    )
    public ResponseEntity<DownloadStatus> getStatus(
            @Parameter(description = "YouTube URL to check status", required = true)
            @RequestParam String url) {
        return ResponseEntity.ok(downloadService.getStatus(url));
    }

    @GetMapping("/download/{filename}")
    @Operation(summary = "Download audio file", description = "Download the audio file by filename")
    public ResponseEntity<Resource> getFile(
            @Parameter(description = "Name of the downloaded file", required = true)
            @PathVariable String filename) {

        Resource fileResource = downloadService.getFile(filename);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(fileResource);
    }

}