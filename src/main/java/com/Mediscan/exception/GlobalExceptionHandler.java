package com.Mediscan.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MedicineNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(MedicineNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", ex.getMessage(),
                "status", 404,
                "timestamp", LocalDateTime.now().toString()
        ));

    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", ex.getMessage(),
                "status", 503,
                "timestamp", LocalDateTime.now().toString()
        ));

    }

    public ResponseEntity<Map<String,Object>> handleGeneral(Exception ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal server error:" + ex.getMessage(),
                "status", 500,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
