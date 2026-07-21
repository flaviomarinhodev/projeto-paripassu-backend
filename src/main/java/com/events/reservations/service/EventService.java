package com.events.reservations.service;

import com.events.reservations.dto.EventResponseDTO;
import com.events.reservations.entity.Event;
import com.events.reservations.exception.EventNotFoundException;
import com.events.reservations.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<EventResponseDTO> listAll() {
        return eventRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponseDTO findById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + id));
        return mapToResponseDTO(event);
    }

    private EventResponseDTO mapToResponseDTO(Event event) {
        return EventResponseDTO.builder()
                .id(event.getId())
                .name(event.getName())
                .eventDate(event.getEventDate())
                .totalSlots(event.getTotalSlots())
                .availableSlots(event.getAvailableSlots())
                .build();
    }
}