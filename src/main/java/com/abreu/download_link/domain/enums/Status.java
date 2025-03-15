package com.abreu.download_link.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "Status",
        description = "Possible states of a download process",
        enumAsRef = true
)
public enum Status {
    @Schema(description = "Download process is initializing")
    STARTING,

    @Schema(description = "Download is currently in progress")
    IN_PROGRESS,

    @Schema(description = "Download successfully completed")
    COMPLETED,

    @Schema(description = "Requested resource not found")
    NOT_FOUND,

    @Schema(description = "Download already exists in system")
    ALREADY_EXISTS
}