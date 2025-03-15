package com.abreu.download_link.domain;

import com.abreu.download_link.domain.enums.Status;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(
        name = "YoutubeResponse",
        description = "Response object for YouTube download operations"
)
public record YoutubeResponse(
        @Schema(
                description = "Human-readable status message",
                example = "Download started successfully"
        )
        String message,

        @Schema(
                description = "Path to the downloaded file",
                example = "/downloads/rick_astley_never_gonna_give_you_up.mp3"
        )
        String filePath,

        @Schema(
                description = "Current status of the download",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Status status,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss", timezone = "America/Sao_Paulo")
        @Schema(
                description = "Timestamp of the operation",
                example = "31-12-2023 23:59:59",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant timestamp
) {
    public YoutubeResponse(String message, String filePath, Status status) {
        this(message, filePath, status, Instant.now());
    }
}
