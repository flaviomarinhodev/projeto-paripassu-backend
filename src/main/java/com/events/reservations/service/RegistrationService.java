package com.events.reservations.service;

import com.events.reservations.dto.RegistrationRequestDTO;
import com.events.reservations.dto.RegistrationResponseDTO;
import com.events.reservations.entity.Event;
import com.events.reservations.entity.Registration;
import com.events.reservations.entity.NotificationOutbox;
import com.events.reservations.enums.NotificationChannel;
import com.events.reservations.enums.RegistrationStatus;
import com.events.reservations.enums.NotificationStatus;
import com.events.reservations.exception.EventNotFoundException;
import com.events.reservations.exception.DuplicateRegistrationException;
import com.events.reservations.exception.SlotsUnavailableException;
import com.events.reservations.repository.EventRepository;
import com.events.reservations.repository.RegistrationRepository;
import com.events.reservations.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationOutboxRepository notificationOutboxRepository;

    /**
     * Slot reservation flow, protected against:
     * - Overselling: atomic conditional decrement at the database level (UPDATE ... WHERE slots > 0)
     * - Duplicate submission (retry/double click): unique idempotency key
     * - Duplicate email: unique constraint (event_id, email)
     * - Duplicate notification sending: outbox decoupled from the business transaction
     */
    @Transactional
    public RegistrationResponseDTO reserveSlot(RegistrationRequestDTO requestDTO, String idempotencyKey) {

        registrationRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(existingRegistration -> {
                    throw new DuplicateRegistrationException(
                            "This request has already been processed for this idempotency key.");
                });

        Event event = eventRepository.findById(requestDTO.getEventId())
                .orElseThrow(() -> new EventNotFoundException(
                        "Event not found: " + requestDTO.getEventId()));

        if (registrationRepository.existsByEventIdAndEmail(event.getId(), requestDTO.getEmail())) {
            throw new DuplicateRegistrationException(
                    "This email is already registered for this event.");
        }

        int rowsAffected = eventRepository.decrementSlotAtomically(event.getId());
        if (rowsAffected == 0) {
            throw new SlotsUnavailableException(
                    "No available slots for event: " + event.getName());
        }

        Registration registration = Registration.builder()
                .event(event)
                .participantName(requestDTO.getParticipantName())
                .email(requestDTO.getEmail())
                .idempotencyKey(idempotencyKey)
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        registration = registrationRepository.save(registration);

        NotificationOutbox notification = NotificationOutbox.builder()
                .registration(registration)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .build();

        notificationOutboxRepository.save(notification);

        log.info("Slot reserved: eventId={}, email={}, registrationId={}",
                event.getId(), requestDTO.getEmail(), registration.getId());

        return mapToResponseDTO(registration);
    }

    private RegistrationResponseDTO mapToResponseDTO(Registration registration) {
        return RegistrationResponseDTO.builder()
                .id(registration.getId())
                .eventId(registration.getEvent().getId())
                .eventName(registration.getEvent().getName())
                .participantName(registration.getParticipantName())
                .email(registration.getEmail())
                .status(registration.getStatus())
                .createdAt(registration.getCreatedAt())
                .build();
    }
}