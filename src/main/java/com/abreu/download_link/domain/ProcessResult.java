package com.abreu.download_link.domain;

public record ProcessResult(
        int exitCode,
        String filepath,
        String error,
        String output
) {
}
