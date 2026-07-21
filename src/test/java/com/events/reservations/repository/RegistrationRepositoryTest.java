package com.events.reservations.repository;

import com.events.reservations.entity.Event;
import com.events.reservations.entity.Registration;
import com.events.reservations.enums.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("RegistrationRepository Tests")
class RegistrationRepositoryTest {

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        testEvent = Event.builder()
                .name("Java Workshop")
                .eventDate(LocalDateTime.now().plusDays(1))
                .totalSlots(50)
                .availableSlots(50)
                .version(1L)
                .build();
        
        testEvent = eventRepository.save(testEvent);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should check if email exists for event")
    void testExistsByEventIdAndEmail_shouldReturnTrue() {
        // Given
        Registration registration = Registration.builder()
                .event(testEvent)
                .participantName("João Silva")
                .email("joao@example.com")
                .idempotencyKey("test-key-1")
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
        registrationRepository.save(registration);
        entityManager.flush();

        // When
        boolean exists = registrationRepository.existsByEventIdAndEmail(
                testEvent.getId(), 
                "joao@example.com"
        );

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email doesn't exist for event")
    void testExistsByEventIdAndEmail_shouldReturnFalse() {
        // Given - no registration with this email
        
        // When
        boolean exists = registrationRepository.existsByEventIdAndEmail(
                testEvent.getId(), 
                "nonexistent@example.com"
        );

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return false for same email but different event")
    void testExistsByEventIdAndEmail_shouldReturnFalseForDifferentEvent() {
        // Given
        Registration registration = Registration.builder()
                .event(testEvent)
                .participantName("João Silva")
                .email("joao@example.com")
                .idempotencyKey("test-key-1")
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
        registrationRepository.save(registration);
        entityManager.flush();

        Event otherEvent = Event.builder()
                .name("Vue.js Workshop")
                .eventDate(LocalDateTime.now().plusDays(2))
                .totalSlots(40)
                .availableSlots(40)
                .version(1L)
                .build();
        otherEvent = eventRepository.save(otherEvent);
        entityManager.flush();

        // When
        boolean exists = registrationRepository.existsByEventIdAndEmail(
                otherEvent.getId(), 
                "joao@example.com"
        );

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should find registration by idempotency key")
    void testFindByIdempotencyKey_shouldFindByKey() {
        // Given
        String idempotencyKey = "unique-idempotency-key";
        Registration registration = Registration.builder()
                .event(testEvent)
                .participantName("João Silva")
                .email("joao@example.com")
                .idempotencyKey(idempotencyKey)
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
        registrationRepository.save(registration);
        entityManager.flush();

        // When
        var found = registrationRepository.findByIdempotencyKey(idempotencyKey);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("joao@example.com");
        assertThat(found.get().getParticipantName()).isEqualTo("João Silva");
    }

    @Test
    @DisplayName("Should return empty when idempotency key doesn't exist")
    void testFindByIdempotencyKey_shouldReturnEmpty() {
        // When
        var found = registrationRepository.findByIdempotencyKey("nonexistent-key");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should enforce unique constraint on (event_id, email)")
    void testUniqueConstraint_shouldRejectDuplicateEmailForSameEvent() {
        // Given
        Registration registration1 = Registration.builder()
                .event(testEvent)
                .participantName("João Silva")
                .email("joao@example.com")
                .idempotencyKey("key-1")
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
        registrationRepository.save(registration1);
        entityManager.flush();

        // When & Then - attempting to save duplicate email for same event should fail
        Registration registration2 = Registration.builder()
                .event(testEvent)
                .participantName("Other Name")
                .email("joao@example.com") // Same email
                .idempotencyKey("key-2")
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> {
            registrationRepository.save(registration2);
            entityManager.flush();
        }).isInstanceOf(Exception.class); // DataIntegrityViolationException
    }

    @Test
    @DisplayName("Should enforce unique constraint on idempotency_key")
    void testUniqueConstraint_shouldRejectDuplicateIdempotencyKey() {
        // Given
        String duplicateKey = "same-idempotency-key";
        Registration registration1 = Registration.builder()
                .event(testEvent)
                .participantName("João Silva")
                .email("joao@example.com")
                .idempotencyKey(duplicateKey)
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
        registrationRepository.save(registration1);
        entityManager.flush();

        // When & Then - attempting to save duplicate idempotency key should fail
        Registration registration2 = Registration.builder()
                .event(testEvent)
                .participantName("Other Name")
                .email("other@example.com")
                .idempotencyKey(duplicateKey) // Same idempotency key
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> {
            registrationRepository.save(registration2);
            entityManager.flush();
        }).isInstanceOf(Exception.class); // DataIntegrityViolationException
    }

    @Test
    @DisplayName("Should allow same email for different events")
    void testAllowSameEmailForDifferentEvents() {
        // Given
        Registration registration1 = Registration.builder()
                .event(testEvent)
                .participantName("João Silva")
                .email("joao@example.com")
                .idempotencyKey("key-1")
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
        registrationRepository.save(registration1);
        entityManager.flush();

        Event otherEvent = Event.builder()
                .name("Vue.js Workshop")
                .eventDate(LocalDateTime.now().plusDays(2))
                .totalSlots(40)
                .availableSlots(40)
                .version(1L)
                .build();
        otherEvent = eventRepository.save(otherEvent);
        entityManager.flush();

        // When & Then - same email but different event should be allowed
        Registration registration2 = Registration.builder()
                .event(otherEvent)
                .participantName("João Silva")
                .email("joao@example.com") // Same email
                .idempotencyKey("key-2")
                .status(RegistrationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        assertThatNoException().isThrownBy(() -> {
            registrationRepository.save(registration2);
            entityManager.flush();
        });

        // Verify both registrations were saved
        assertThat(registrationRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Should count registrations by event")
    void testCountRegistrationsByEvent() {
        // Given
        for (int i = 1; i <= 5; i++) {
            Registration registration = Registration.builder()
                    .event(testEvent)
                    .participantName("Participant " + i)
                    .email("participant" + i + "@example.com")
                    .idempotencyKey("key-" + i)
                    .status(RegistrationStatus.CONFIRMED)
                    .createdAt(LocalDateTime.now())
                    .build();
            registrationRepository.save(registration);
        }
        entityManager.flush();

        // When
        long count = registrationRepository.count();

        // Then
        assertThat(count).isEqualTo(5L);
    }
}
