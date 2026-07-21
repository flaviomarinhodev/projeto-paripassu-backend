package com.events.reservations.controller;

import com.events.reservations.dto.RegistrationRequestDTO;
import com.events.reservations.dto.RegistrationResponseDTO;
import com.events.reservations.enums.RegistrationStatus;
import com.events.reservations.exception.DuplicateRegistrationException;
import com.events.reservations.exception.EventNotFoundException;
import com.events.reservations.exception.SlotsUnavailableException;
import com.events.reservations.service.RegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("RegistrationController Tests")
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    private RegistrationRequestDTO requestDTO;
    private RegistrationResponseDTO responseDTO;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        idempotencyKey = UUID.randomUUID().toString();

        requestDTO = RegistrationRequestDTO.builder()
                .eventId(1L)
                .participantName("João Silva")
                .email("joao@example.com")
                .build();

        responseDTO = RegistrationResponseDTO.builder()
                .id(10L)
                .eventId(1L)
                .eventName("Java Workshop")
                .participantName("João Silva")
                .email("joao@example.com")
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should return 201 CREATED when registration successful")
    void testReserveSlot_shouldReturn201() throws Exception {
        // Given
        when(registrationService.reserveSlot(any(RegistrationRequestDTO.class), anyString()))
                .thenReturn(responseDTO);

        // When & Then
        mockMvc.perform(post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.eventId").value(1L))
                .andExpect(jsonPath("$.email").value("joao@example.com"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(registrationService).reserveSlot(any(RegistrationRequestDTO.class), eq(idempotencyKey));
    }

    @Test
    @DisplayName("Should generate Idempotency-Key if not provided")
    void testReserveSlot_shouldGenerateIdempotencyKeyIfNotProvided() throws Exception {
        // Given
        when(registrationService.reserveSlot(any(RegistrationRequestDTO.class), anyString()))
                .thenReturn(responseDTO);

        // When & Then
        mockMvc.perform(post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated());

        // Verify that service was called with some idempotency key (generated UUID)
        verify(registrationService).reserveSlot(any(RegistrationRequestDTO.class), anyString());
    }

    @Test
    @DisplayName("Should return 400 BAD REQUEST when request is invalid (missing email)")
    void testReserveSlot_shouldReturn400WhenRequestInvalid() throws Exception {
        // Given
        RegistrationRequestDTO invalidRequest = RegistrationRequestDTO.builder()
                .eventId(1L)
                .participantName("João Silva")
                // email is missing
                .build();

        // When & Then
        mockMvc.perform(post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).reserveSlot(any(), any());
    }

    @Test
    @DisplayName("Should return 409 CONFLICT when duplicate registration")
    void testReserveSlot_shouldReturn409WhenDuplicate() throws Exception {
        // Given
        when(registrationService.reserveSlot(any(RegistrationRequestDTO.class), anyString()))
                .thenThrow(new DuplicateRegistrationException("Email already registered"));

        // When & Then
        mockMvc.perform(post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isConflict());

        verify(registrationService).reserveSlot(any(RegistrationRequestDTO.class), eq(idempotencyKey));
    }

    @Test
    @DisplayName("Should return 404 NOT FOUND when event doesn't exist")
    void testReserveSlot_shouldReturn404WhenEventNotFound() throws Exception {
        // Given
        when(registrationService.reserveSlot(any(RegistrationRequestDTO.class), anyString()))
                .thenThrow(new EventNotFoundException("Event not found"));

        // When & Then
        mockMvc.perform(post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());

        verify(registrationService).reserveSlot(any(RegistrationRequestDTO.class), eq(idempotencyKey));
    }

    @Test
    @DisplayName("Should return 409 CONFLICT when no slots available")
    void testReserveSlot_shouldReturn409WhenNoSlots() throws Exception {
        // Given
        when(registrationService.reserveSlot(any(RegistrationRequestDTO.class), anyString()))
                .thenThrow(new SlotsUnavailableException("No slots available"));

        // When & Then
        mockMvc.perform(post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isConflict());

        verify(registrationService).reserveSlot(any(RegistrationRequestDTO.class), eq(idempotencyKey));
    }

    @Test
    @DisplayName("Should use provided Idempotency-Key header")
    void testReserveSlot_shouldUseProvidedIdempotencyKey() throws Exception {
        // Given
        String customKey = "custom-idempotency-key-123";
        when(registrationService.reserveSlot(any(RegistrationRequestDTO.class), anyString()))
                .thenReturn(responseDTO);

        // When & Then
        mockMvc.perform(post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", customKey)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated());

        verify(registrationService).reserveSlot(any(RegistrationRequestDTO.class), eq(customKey));
    }

    @Test
    @DisplayName("Should return response DTO with all required fields")
    void testReserveSlot_shouldReturnCompleteResponse() throws Exception {
        // Given
        when(registrationService.reserveSlot(any(RegistrationRequestDTO.class), anyString()))
                .thenReturn(responseDTO);

        // When & Then
        mockMvc.perform(post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.eventName").exists())
                .andExpect(jsonPath("$.participantName").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }
}
