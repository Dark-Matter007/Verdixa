package com.leetcode.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import com.leetcode.backend.service.EmailNotVerifiedException;
import com.leetcode.backend.service.OtpVerificationException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, String>> emailNotVerified(EmailNotVerifiedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("code", "EMAIL_VERIFICATION_REQUIRED", "message", exception.getMessage()));
    }
    @ExceptionHandler(OtpVerificationException.class)
    public ResponseEntity<Map<String, String>> invalidOtp(OtpVerificationException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalidInput(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", "Please check the submitted information."));
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> statusException(org.springframework.web.server.ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", exception.getReason()==null?"Request failed.":exception.getReason()));
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> forbidden(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> notFound(RuntimeException exception) {
        String message = exception.getMessage() == null ? "Request failed." : exception.getMessage();
        HttpStatus status = message.toLowerCase().contains("not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
