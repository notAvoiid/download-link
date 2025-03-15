package com.abreu.download_link.domain;

import com.abreu.download_link.domain.enums.Status;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "DownloadStatus",
        description = "Represents the current status of a download process"
)
public record DownloadStatus(
        @Schema(
                description = "Current status of the download",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Status status,

        @Schema(
                description = "Additional status message or details",
                example = "Download is 50% complete"
        )
        String message
) {
}