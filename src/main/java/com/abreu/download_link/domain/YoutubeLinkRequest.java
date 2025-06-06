package com.abreu.download_link.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(
        name = "YoutubeLinkRequest",
        description = "Request object containing YouTube URL for processing"
)
public record YoutubeLinkRequest(
        @Pattern(
                regexp = "^https?://(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/)[a-zA-Z0-9_-]{11}$",
                message = "Invalid YouTube URL format. Please provide a valid URL."
        )
        @Schema(
                description = "Valid YouTube URL to process",
                example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String url
) {}
