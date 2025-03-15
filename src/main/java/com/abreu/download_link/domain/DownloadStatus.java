package com.abreu.download_link.domain;

import com.abreu.download_link.domain.enums.Status;

public record DownloadStatus(
        Status status,
        String message
) {
}
