package com.abreu.download_link.exceptions;

public class DownloadFailedException extends RuntimeException {
    public DownloadFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadFailedException(String message) {
        super(message);


    }
}
