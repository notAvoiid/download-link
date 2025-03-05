package com.abreu.download_link.exceptions;

public class InvalidYoutubeUrlException extends RuntimeException {
    public InvalidYoutubeUrlException(String message) {
        super(message);
    }
}
