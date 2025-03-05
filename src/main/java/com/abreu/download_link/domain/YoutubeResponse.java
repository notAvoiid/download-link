package com.abreu.download_link.domain;

import com.abreu.download_link.domain.enums.Status;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record YoutubeResponse(
        String message,
        String filePath,
        Status status,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss", timezone = "America/Sao_Paulo")
        Instant timestamp
) {

    public YoutubeResponse(String message, String filePath, Status status) {
        this(message, filePath, status, Instant.now());
    }

}
