package com.abreu.download_link.domain;

import jakarta.validation.constraints.Pattern;

public record YoutubeLinkRequest(
        @Pattern(
                regexp = "^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.*" +
                        "|" +
                        "^https?://(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})",
                message = "Invalid YouTube URL format. Please provide a valid URL."
        ) String url
) {
}
