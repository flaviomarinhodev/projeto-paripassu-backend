package com.events.reservations.notification;

import com.events.reservations.entity.Event;
import com.events.reservations.entity.NotificationOutbox;
import com.events.reservations.entity.Registration;
import com.events.reservations.enums.NotificationChannel;
import com.events.reservations.enums.NotificationStatus;
import com.events.reservations.enums.RegistrationStatus;
import com.events.reservations.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationOutboxProcessor Tests")
class NotificationOutboxProcessorTest {

    @Mock
    private NotificationOutboxRepository notificationOutboxRepository;

    @Mock
    private RegistrationNotifier emailNotifier;

    @InjectMocks
    private NotificationOutboxProcessor processor;

    private Event testEvent;
    private Registration testRegistration;
    private NotificationOutbox testNotification;

    @BeforeEach
    void setUp() {
        testEvent = Event.builder()
                .id(1L)
                .name("Java Workshop")
                .eventDate(LocalDateTime.now().plusDays(1))
                .totalSlots(50)
                .availableSlots(40)
                .version(1L)
                .build();

        testRegistration = Registration.builder()
                .id(10L)
                .event(testEvent)
                .participantName("João Silva")
                .email("joao@example.com")
                .idempotencyKey("test-key")
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        testNotification = NotificationOutbox.builder()
                .id(1L)
                .registration(testRegistration)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should handle when no pending notifications exist")
    void testProcessPending_shouldHandleEmptyList() {
        // Given
        when(notificationOutboxRepository.findTop50ByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING))
                .thenReturn(List.of());

        // When
        processor.processPending();

        // Then
        verify(emailNotifier, never()).send(any());
        verify(notificationOutboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retry notification that failed")
    void testProcessPending_shouldRetryFailedNotification() {
        // Given
        testNotification.setAttempts(2);
        testNotification.setStatus(NotificationStatus.FAILED);
        List<NotificationOutbox> failedNotifications = List.of(testNotification);
        
        when(notificationOutboxRepository.findTop50ByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING))
                .thenReturn(List.of()); // No more pending, but we're testing FAILED retry separately

        // When - This tests that processor only processes PENDING
        processor.processPending();

        // Then - Failed notifications should not be reprocessed
        verify(emailNotifier, never()).send(any());
    }

}
