package com.abreu.download_link.exceptions.handler;

import com.abreu.download_link.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String ERROR_PREFIX = "API Error";

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleAllExceptions(Exception ex, HttpServletRequest request) {
        logError(ex, request);
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .contentType(APPLICATION_JSON)
                .body(buildErrorMessage(request, INTERNAL_SERVER_ERROR, ex.getMessage()));
    }

    @ExceptionHandler(DownloadFailedException.class)
    public ResponseEntity<ErrorMessage> handleDownloadFailedException(DownloadFailedException ex, HttpServletRequest request) {
        log.error("{} - URI: {}", ERROR_PREFIX, request != null ? request.getRequestURI() : "N/A", ex);
        return ResponseEntity
                .status(BAD_REQUEST)
                .contentType(APPLICATION_JSON)
                .body(buildErrorMessage(request, BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(FileAlreadyExistsException.class)
    public ResponseEntity<ErrorMessage> handleFileAlreadyExistsException(FileAlreadyExistsException ex, HttpServletRequest request) {
        logError(ex, request);
        return ResponseEntity
                .status(CONFLICT)
                .contentType(APPLICATION_JSON)
                .body(buildErrorMessage(request, CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(InvalidYoutubeUrlException.class)
    public ResponseEntity<ErrorMessage> handleInvalidYoutubeUrlException(InvalidYoutubeUrlException ex, HttpServletRequest request) {
        logError(ex, request);
        return ResponseEntity
                .status(BAD_REQUEST)
                .contentType(APPLICATION_JSON)
                .body(buildErrorMessage(request, BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(FileNameRetrievalException.class)
    public ResponseEntity<ErrorMessage> handleFileNameRetrievalException(FileNameRetrievalException ex, HttpServletRequest request) {
        logError(ex, request);
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .contentType(APPLICATION_JSON)
                .body(buildErrorMessage(request, INTERNAL_SERVER_ERROR, ex.getMessage()));
    }

    @ExceptionHandler(MalformedURLException.class)
    public ResponseEntity<ErrorMessage> handleMalformedURLException(MalformedURLException ex, HttpServletRequest request) {
        logError(ex, request);
        return ResponseEntity
                .status(BAD_REQUEST)
                .contentType(APPLICATION_JSON)
                .body(buildErrorMessage(request, BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public final ResponseEntity<ErrorMessage> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        if (request == null) {
            log.error("{} - URI: {}", ERROR_PREFIX, "N/A", ex);
            return ResponseEntity.badRequest().contentType(APPLICATION_JSON).body(new ErrorMessage(BAD_REQUEST, ex.getMessage()));
        }

        log.error("{} - URI: {}", ERROR_PREFIX, request.getRequestURI(), ex);
        return ResponseEntity
                .badRequest()
                .contentType(APPLICATION_JSON)
                .body(new ErrorMessage(request, BAD_REQUEST, "Validation error in fields", ex.getBindingResult()));

    }

    private ErrorMessage buildErrorMessage(HttpServletRequest request, HttpStatus status, String message) {
        return request != null ? new ErrorMessage(request, status, message) : new ErrorMessage(status, message);
    }

    private void logError(Exception ex, HttpServletRequest request) {
        log.error("{} - URI: {} - Error: {}", ERROR_PREFIX,
                request != null ? request.getRequestURI() : "N/A", ex.getMessage(), ex);
    }

}

