package com.abreu.download_link.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ProcessResult",
        description = "Result of a download process execution"
)
public record ProcessResult(
        @Schema(
                description = "Process exit code (0 = success)",
                example = "0"
        )
        int exitCode,

        @Schema(
                description = "Path to the downloaded file",
                example = "/tmp/youtube_video.mp3"
        )
        String filepath,

        @Schema(
                description = "Error output if any occurred",
                example = "FFmpeg conversion failed"
        )
        String error,

        @Schema(
                description = "Standard process output",
                example = "Download completed successfully"
        )
        String output
) {
}