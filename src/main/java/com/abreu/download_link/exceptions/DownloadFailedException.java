package com.abreu.download_link.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.ACCEPTED)
public class DownloadFailedException extends RuntimeException {
    public DownloadFailedException(String message) {
        super(message);
    }
}
