package com.events.reservations.controller;

import com.events.reservations.dto.EventResponseDTO;
import com.events.reservations.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public List<EventResponseDTO> listAll() {
        return eventService.listAll();
    }

    @GetMapping("/{id}")
    public EventResponseDTO findById(@PathVariable Long id) {
        return eventService.findById(id);
    }
}