package com.events.reservations.service;

import com.events.reservations.dto.RegistrationRequestDTO;
import com.events.reservations.dto.RegistrationResponseDTO;
import com.events.reservations.entity.Event;
import com.events.reservations.entity.NotificationOutbox;
import com.events.reservations.entity.Registration;
import com.events.reservations.enums.NotificationChannel;
import com.events.reservations.enums.NotificationStatus;
import com.events.reservations.enums.RegistrationStatus;
import com.events.reservations.exception.DuplicateRegistrationException;
import com.events.reservations.exception.EventNotFoundException;
import com.events.reservations.exception.SlotsUnavailableException;
import com.events.reservations.repository.EventRepository;
import com.events.reservations.repository.NotificationOutboxRepository;
import com.events.reservations.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService Tests")
class RegistrationServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private NotificationOutboxRepository notificationOutboxRepository;

    @InjectMocks
    private RegistrationService registrationService;

    private Event testEvent;
    private RegistrationRequestDTO requestDTO;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        testEvent = Event.builder()
                .id(1L)
                .name("Java Workshop")
                .eventDate(LocalDateTime.now().plusDays(1))
                .totalSlots(50)
                .availableSlots(10)
                .version(1L)
                .build();

        requestDTO = RegistrationRequestDTO.builder()
                .eventId(1L)
                .participantName("João Silva")
                .email("joao@example.com")
                .build();

        idempotencyKey = "test-idempotency-key";
    }

    @Test
    @DisplayName("Should successfully reserve slot when slots are available")
    void testReserveSlot_shouldSuccessfullyReserveWhenSlotsAvailable() {
        // Given
        when(registrationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByEventIdAndEmail(1L, "joao@example.com"))
                .thenReturn(false);
        when(eventRepository.decrementSlotAtomically(1L))
                .thenReturn(1); // Successfully decremented

        Registration savedRegistration = Registration.builder()
                .id(10L)
                .event(testEvent)
                .participantName("João Silva")
                .email("joao@example.com")
                .idempotencyKey(idempotencyKey)
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        when(registrationRepository.save(any(Registration.class)))
                .thenReturn(savedRegistration);

        when(notificationOutboxRepository.save(any(NotificationOutbox.class)))
                .thenReturn(NotificationOutbox.builder()
                        .id(1L)
                        .registration(savedRegistration)
                        .channel(NotificationChannel.EMAIL)
                        .status(NotificationStatus.PENDING)
                        .attempts(0)
                        .createdAt(LocalDateTime.now())
                        .build());

        // When
        RegistrationResponseDTO response = registrationService.reserveSlot(requestDTO, idempotencyKey);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("joao@example.com");
        assertThat(response.getEventName()).isEqualTo("Java Workshop");
        assertThat(response.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);

        verify(eventRepository).decrementSlotAtomically(1L);
        verify(registrationRepository).save(any(Registration.class));
        verify(notificationOutboxRepository).save(any(NotificationOutbox.class));
    }

    @Test
    @DisplayName("Should throw EventNotFoundException when event doesn't exist")
    void testReserveSlot_shouldThrowWhenEventNotFound() {
        // Given
        when(registrationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(eventRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> registrationService.reserveSlot(requestDTO, idempotencyKey))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("Event not found");

        verify(registrationRepository, never()).save(any());
        verify(notificationOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateRegistrationException when email already registered")
    void testReserveSlot_shouldThrowWhenEmailDuplicate() {
        // Given
        when(registrationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByEventIdAndEmail(1L, "joao@example.com"))
                .thenReturn(true); // Email already registered

        // When & Then
        assertThatThrownBy(() -> registrationService.reserveSlot(requestDTO, idempotencyKey))
                .isInstanceOf(DuplicateRegistrationException.class)
                .hasMessageContaining("already registered");

        verify(eventRepository, never()).decrementSlotAtomically(anyLong());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw SlotsUnavailableException when no slots available")
    void testReserveSlot_shouldThrowWhenSlotsUnavailable() {
        // Given
        when(registrationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByEventIdAndEmail(1L, "joao@example.com"))
                .thenReturn(false);
        when(eventRepository.decrementSlotAtomically(1L))
                .thenReturn(0); // No slots available

        // When & Then
        assertThatThrownBy(() -> registrationService.reserveSlot(requestDTO, idempotencyKey))
                .isInstanceOf(SlotsUnavailableException.class)
                .hasMessageContaining("No available slots");

        verify(registrationRepository, never()).save(any());
        verify(notificationOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should be idempotent - same idempotency key should not reprocess")
    void testReserveSlot_shouldBeIdempotent() {
        // Given - registration already exists with this idempotency key
        Registration existingRegistration = Registration.builder()
                .id(5L)
                .event(testEvent)
                .participantName("João Silva")
                .email("joao@example.com")
                .idempotencyKey(idempotencyKey)
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        when(registrationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingRegistration));

        // When & Then
        assertThatThrownBy(() -> registrationService.reserveSlot(requestDTO, idempotencyKey))
                .isInstanceOf(DuplicateRegistrationException.class)
                .hasMessageContaining("already been processed");

        verify(eventRepository, never()).decrementSlotAtomically(anyLong());
    }

    @Test
    @DisplayName("Should create NotificationOutbox entry after successful registration")
    void testReserveSlot_shouldCreateNotificationOutbox() {
        // Given
        when(registrationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(testEvent));
        when(registrationRepository.existsByEventIdAndEmail(1L, "joao@example.com"))
                .thenReturn(false);
        when(eventRepository.decrementSlotAtomically(1L))
                .thenReturn(1);

        Registration savedRegistration = Registration.builder()
                .id(10L)
                .event(testEvent)
                .participantName("João Silva")
                .email("joao@example.com")
                .idempotencyKey(idempotencyKey)
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        when(registrationRepository.save(any(Registration.class)))
                .thenReturn(savedRegistration);
        when(notificationOutboxRepository.save(any(NotificationOutbox.class)))
                .thenReturn(NotificationOutbox.builder()
                        .id(1L)
                        .registration(savedRegistration)
                        .channel(NotificationChannel.EMAIL)
                        .status(NotificationStatus.PENDING)
                        .attempts(0)
                        .createdAt(LocalDateTime.now())
                        .build());

        // When
        registrationService.reserveSlot(requestDTO, idempotencyKey);

        // Then
        ArgumentCaptor<NotificationOutbox> notificationCaptor = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(notificationOutboxRepository).save(notificationCaptor.capture());

        NotificationOutbox savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(savedNotification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(savedNotification.getAttempts()).isEqualTo(0);
    }
}
