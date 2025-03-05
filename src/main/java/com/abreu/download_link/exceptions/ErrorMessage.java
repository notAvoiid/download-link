package com.abreu.download_link.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.util.Date;

@ToString
@Getter
public class ErrorMessage {

    private String path;
    private String method;
    private final int status;
    private final String statusText;
    private final String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss", timezone = "America/Sao_Paulo")
    private final Date timestamp;

    public ErrorMessage(HttpStatus status, String message) {
        this.status = status.value();
        this.statusText = status.getReasonPhrase();
        this.timestamp = new Date();
        this.message = message;
    }

    public ErrorMessage(HttpServletRequest request, HttpStatus status, String message) {
        this.path = request.getRequestURI();
        this.method = request.getMethod();
        this.status = status.value();
        this.statusText = status.getReasonPhrase();
        this.timestamp = new Date();
        this.message = message;
    }
}