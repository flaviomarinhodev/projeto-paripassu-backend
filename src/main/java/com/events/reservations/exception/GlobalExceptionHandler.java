package com.events.reservations.exception;

import com.events.reservations.dto.ErrorResponseDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SlotsUnavailableException.class)
    public ResponseEntity<ErrorResponseDTO> handleSlotsUnavailable(SlotsUnavailableException ex) {
        return buildResponse(HttpStatus.CONFLICT, "SLOTS_UNAVAILABLE", ex.getMessage());
    }

    @ExceptionHandler(DuplicateRegistrationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicateRegistration(DuplicateRegistrationException ex) {
        return buildResponse(HttpStatus.CONFLICT, "DUPLICATE_REGISTRATION", ex.getMessage());
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleEventNotFound(EventNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return buildResponse(HttpStatus.CONFLICT, "DUPLICATE_REGISTRATION",
                "A registration with this email already exists for this event.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid data");
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_DATA", message);
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, String code, String message) {
        ErrorResponseDTO body = ErrorResponseDTO.builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}