package com.events.reservations.controller;

import com.events.reservations.dto.RegistrationRequestDTO;
import com.events.reservations.dto.RegistrationResponseDTO;
import com.events.reservations.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    public ResponseEntity<RegistrationResponseDTO> reserveSlot(
            @Valid @RequestBody RegistrationRequestDTO requestDTO,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader) {

        String idempotencyKey = (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank())
                ? idempotencyKeyHeader
                : UUID.randomUUID().toString();

        RegistrationResponseDTO response = registrationService.reserveSlot(requestDTO, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}