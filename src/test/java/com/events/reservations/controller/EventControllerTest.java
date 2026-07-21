package com.events.reservations.controller;

import com.events.reservations.dto.EventResponseDTO;
import com.events.reservations.exception.EventNotFoundException;
import com.events.reservations.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("EventController Tests")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    private EventResponseDTO event1;
    private EventResponseDTO event2;

    @BeforeEach
    void setUp() {
        event1 = EventResponseDTO.builder()
                .id(1L)
                .name("Java Workshop")
                .eventDate(LocalDateTime.of(2026, 8, 15, 10, 0))
                .totalSlots(50)
                .availableSlots(50)
                .build();

        event2 = EventResponseDTO.builder()
                .id(2L)
                .name("Microservices Masterclass")
                .eventDate(LocalDateTime.of(2026, 8, 22, 14, 0))
                .totalSlots(30)
                .availableSlots(15)
                .build();
    }

    @Test
    @DisplayName("Should return 200 with all events")
    void testListAll_shouldReturn200WithEvents() throws Exception {
        // Given
        List<EventResponseDTO> events = List.of(event1, event2);
        when(eventService.listAll()).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Java Workshop"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("Microservices Masterclass"));

        verify(eventService).listAll();
    }

    @Test
    @DisplayName("Should return 200 with empty list when no events")
    void testListAll_shouldReturn200WithEmptyList() throws Exception {
        // Given
        when(eventService.listAll()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(eventService).listAll();
    }

    @Test
    @DisplayName("Should return event with correct slot information")
    void testListAll_shouldReturnEventsWithSlotInfo() throws Exception {
        // Given
        List<EventResponseDTO> events = List.of(event1);
        when(eventService.listAll()).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalSlots").value(50))
                .andExpect(jsonPath("$[0].availableSlots").value(50))
                .andExpect(jsonPath("$[0].eventDate").exists());

        verify(eventService).listAll();
    }

    @Test
    @DisplayName("Should return 200 with event when found by ID")
    void testFindById_shouldReturn200WithEvent() throws Exception {
        // Given
        when(eventService.findById(1L)).thenReturn(event1);

        // When & Then
        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Java Workshop"))
                .andExpect(jsonPath("$.totalSlots").value(50))
                .andExpect(jsonPath("$.availableSlots").value(50));

        verify(eventService).findById(1L);
    }

    @Test
    @DisplayName("Should return 404 when event not found by ID")
    void testFindById_shouldReturn404WhenNotFound() throws Exception {
        // Given
        when(eventService.findById(999L))
                .thenThrow(new EventNotFoundException("Event not found: 999"));

        // When & Then
        mockMvc.perform(get("/api/events/999"))
                .andExpect(status().isNotFound());

        verify(eventService).findById(999L);
    }

    @Test
    @DisplayName("Should return event with updated slots after registration")
    void testFindById_shouldReturnEventWithUpdatedSlots() throws Exception {
        // Given
        EventResponseDTO eventWithReducedSlots = EventResponseDTO.builder()
                .id(2L)
                .name("Vue.js Workshop")
                .eventDate(LocalDateTime.of(2026, 9, 1, 15, 0))
                .totalSlots(40)
                .availableSlots(5) // Reduced from 40
                .build();

        when(eventService.findById(2L)).thenReturn(eventWithReducedSlots);

        // When & Then
        mockMvc.perform(get("/api/events/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").value(40))
                .andExpect(jsonPath("$.availableSlots").value(5));

        verify(eventService).findById(2L);
    }

    @Test
    @DisplayName("Should return all required fields in event response")
    void testFindById_shouldReturnAllRequiredFields() throws Exception {
        // Given
        when(eventService.findById(1L)).thenReturn(event1);

        // When & Then
        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.eventDate").exists())
                .andExpect(jsonPath("$.totalSlots").exists())
                .andExpect(jsonPath("$.availableSlots").exists());

        verify(eventService).findById(1L);
    }

    @Test
    @DisplayName("Should handle invalid ID format gracefully")
    void testFindById_shouldHandle400ForInvalidId() throws Exception {
        // When & Then - Spring will return 400 for invalid path variable format
        mockMvc.perform(get("/api/events/invalid-id"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).findById(anyLong());
    }
}
