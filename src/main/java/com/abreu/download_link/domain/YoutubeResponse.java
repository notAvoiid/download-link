package com.abreu.download_link.domain;

public record YoutubeResponse(
        boolean success,
        String message,
        String filePath
) {
}
