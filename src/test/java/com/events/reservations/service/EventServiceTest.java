package com.events.reservations.service;

import com.events.reservations.dto.EventResponseDTO;
import com.events.reservations.entity.Event;
import com.events.reservations.exception.EventNotFoundException;
import com.events.reservations.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Tests")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private Event event1;
    private Event event2;

    @BeforeEach
    void setUp() {
        event1 = Event.builder()
                .id(1L)
                .name("Java Workshop")
                .eventDate(LocalDateTime.of(2026, 8, 15, 10, 0))
                .totalSlots(50)
                .availableSlots(50)
                .version(1L)
                .build();

        event2 = Event.builder()
                .id(2L)
                .name("Microservices Masterclass")
                .eventDate(LocalDateTime.of(2026, 8, 22, 14, 0))
                .totalSlots(30)
                .availableSlots(15)
                .version(1L)
                .build();
    }

    @Test
    @DisplayName("Should return all events")
    void testListAll_shouldReturnAllEvents() {
        // Given
        List<Event> events = List.of(event1, event2);
        when(eventRepository.findAll()).thenReturn(events);

        // When
        List<EventResponseDTO> result = eventService.listAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("id")
                .containsExactly(1L, 2L);
        assertThat(result).extracting("name")
                .containsExactly("Java Workshop", "Microservices Masterclass");

        verify(eventRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no events exist")
    void testListAll_shouldReturnEmptyList() {
        // Given
        when(eventRepository.findAll()).thenReturn(List.of());

        // When
        List<EventResponseDTO> result = eventService.listAll();

        // Then
        assertThat(result).isEmpty();
        verify(eventRepository).findAll();
    }

    @Test
    @DisplayName("Should properly map Event to EventResponseDTO")
    void testListAll_shouldMapToDTO() {
        // Given
        List<Event> events = List.of(event1);
        when(eventRepository.findAll()).thenReturn(events);

        // When
        List<EventResponseDTO> result = eventService.listAll();

        // Then
        assertThat(result).hasSize(1);
        EventResponseDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Java Workshop");
        assertThat(dto.getEventDate()).isEqualTo(LocalDateTime.of(2026, 8, 15, 10, 0));
        assertThat(dto.getTotalSlots()).isEqualTo(50);
        assertThat(dto.getAvailableSlots()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should find event by ID when exists")
    void testFindById_shouldReturnEventWhenExists() {
        // Given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));

        // When
        EventResponseDTO result = eventService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Java Workshop");
        assertThat(result.getTotalSlots()).isEqualTo(50);
        assertThat(result.getAvailableSlots()).isEqualTo(50);

        verify(eventRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw EventNotFoundException when event doesn't exist")
    void testFindById_shouldThrowWhenNotFound() {
        // Given
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.findById(999L))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("Event not found");

        verify(eventRepository).findById(999L);
    }

    @Test
    @DisplayName("Should correctly map event with reduced available slots")
    void testFindById_shouldMapEventWithReducedSlots() {
        // Given
        Event eventWithReducedSlots = Event.builder()
                .id(2L)
                .name("Vue.js Workshop")
                .eventDate(LocalDateTime.of(2026, 9, 1, 15, 0))
                .totalSlots(40)
                .availableSlots(5)
                .version(1L)
                .build();

        when(eventRepository.findById(2L)).thenReturn(Optional.of(eventWithReducedSlots));

        // When
        EventResponseDTO result = eventService.findById(2L);

        // Then
        assertThat(result.getTotalSlots()).isEqualTo(40);
        assertThat(result.getAvailableSlots()).isEqualTo(5);
    }
}
