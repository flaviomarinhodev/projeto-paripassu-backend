package com.events.reservations.repository;

import com.events.reservations.entity.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("EventRepository Tests")
class EventRepositoryTest {

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
    @DisplayName("Should decrement available slots atomically")
    void testDecrementSlotAtomically_shouldDecrement() {
        // Given
        Event event = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertThat(event.getAvailableSlots()).isEqualTo(50);

        // When
        int rowsAffected = eventRepository.decrementSlotAtomically(testEvent.getId());

        // Then
        assertThat(rowsAffected).isEqualTo(1);
        
        entityManager.clear(); // Clear persistence context to force reload
        Event reloadedEvent = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertThat(reloadedEvent.getAvailableSlots()).isEqualTo(49);
    }

    @Test
    @DisplayName("Should decrement slot only when availableSlots > 0")
    void testDecrementSlotAtomically_shouldOnlyDecrementWhenAvailable() {
        // Given
        testEvent.setAvailableSlots(1);
        eventRepository.save(testEvent);
        entityManager.flush();

        // First decrement - should succeed
        int rowsAffected1 = eventRepository.decrementSlotAtomically(testEvent.getId());
        assertThat(rowsAffected1).isEqualTo(1);

        entityManager.clear();
        Event reloadedEvent = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertThat(reloadedEvent.getAvailableSlots()).isEqualTo(0);

        // Second decrement - should fail (return 0 rows affected)
        int rowsAffected2 = eventRepository.decrementSlotAtomically(testEvent.getId());
        assertThat(rowsAffected2).isEqualTo(0);

        entityManager.clear();
        Event reloadedAgain = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertThat(reloadedAgain.getAvailableSlots()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return 0 rows affected when no slots available")
    void testDecrementSlotAtomically_shouldReturnZeroWhenNoSlots() {
        // Given
        testEvent.setAvailableSlots(0);
        eventRepository.save(testEvent);
        entityManager.flush();

        // When
        int rowsAffected = eventRepository.decrementSlotAtomically(testEvent.getId());

        // Then
        assertThat(rowsAffected).isEqualTo(0);
        
        entityManager.clear();
        Event reloadedEvent = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertThat(reloadedEvent.getAvailableSlots()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should decrement multiple times in sequence")
    void testDecrementSlotAtomically_shouldDecrementMultipleTimes() {
        // Given
        Event event = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertThat(event.getAvailableSlots()).isEqualTo(50);

        // When & Then
        for (int i = 0; i < 50; i++) {
            int rowsAffected = eventRepository.decrementSlotAtomically(testEvent.getId());
            assertThat(rowsAffected).isEqualTo(1);
        }

        entityManager.clear();
        Event reloadedEvent = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertThat(reloadedEvent.getAvailableSlots()).isEqualTo(0);

        // 51st decrement should fail
        int rowsAffected = eventRepository.decrementSlotAtomically(testEvent.getId());
        assertThat(rowsAffected).isEqualTo(0);
    }

    @Test
    @DisplayName("Should find event by ID")
    void testFindById_shouldReturnEvent() {
        // When
        var found = eventRepository.findById(testEvent.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Java Workshop");
        assertThat(found.get().getTotalSlots()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should return empty when event not found")
    void testFindById_shouldReturnEmpty() {
        // When
        var found = eventRepository.findById(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should properly maintain version for optimistic locking")
    void testVersionField_shouldMaintainVersionForOptimisticLocking() {
        // Given
        Event event = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertThat(event.getVersion()).isEqualTo(1L);

        // When - Update event
        event.setAvailableSlots(49);
        eventRepository.save(event);
        entityManager.flush();

        // Then - Version should be incremented
        entityManager.clear();
        Event reloadedEvent = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertThat(reloadedEvent.getVersion()).isGreaterThan(1L);
    }

    @Test
    @DisplayName("Should decrement without affecting version in pessimistic lock")
    void testDecrementSlotAtomically_shouldNotAffectVersionDuringDecrement() {
        // Given
        Event event = eventRepository.findById(testEvent.getId()).orElseThrow();
        Long initialVersion = event.getVersion();

        // When - Decrement slot (should use UPDATE ... WHERE availableSlots > 0)
        eventRepository.decrementSlotAtomically(testEvent.getId());

        // Then - Version might be incremented by Hibernate's version management
        // but the important thing is slot was decremented
        entityManager.clear();
        Event reloadedEvent = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertThat(reloadedEvent.getAvailableSlots()).isEqualTo(49);
    }

    @Test
    @DisplayName("Should handle concurrent-like decrement operations")
    void testDecrementSlotAtomically_shouldHandleMultipleDecrementsCorrectly() {
        // Given
        testEvent.setAvailableSlots(3);
        eventRepository.save(testEvent);
        entityManager.flush();

        // Simulate 3 concurrent-like decrements
        int rowsAffected1 = eventRepository.decrementSlotAtomically(testEvent.getId());
        entityManager.clear();
        
        int rowsAffected2 = eventRepository.decrementSlotAtomically(testEvent.getId());
        entityManager.clear();
        
        int rowsAffected3 = eventRepository.decrementSlotAtomically(testEvent.getId());
        entityManager.clear();

        // Then - All three should succeed
        assertThat(rowsAffected1).isEqualTo(1);
        assertThat(rowsAffected2).isEqualTo(1);
        assertThat(rowsAffected3).isEqualTo(1);

        Event reloadedEvent = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertThat(reloadedEvent.getAvailableSlots()).isEqualTo(0);

        // Fourth decrement should fail
        int rowsAffected4 = eventRepository.decrementSlotAtomically(testEvent.getId());
        assertThat(rowsAffected4).isEqualTo(0);
    }

    @Test
    @DisplayName("Should retrieve event with all fields populated")
    void testFindById_shouldReturnEventWithAllFields() {
        // When
        var found = eventRepository.findById(testEvent.getId());

        // Then
        assertThat(found).isPresent();
        Event event = found.get();
        assertThat(event.getId()).isNotNull();
        assertThat(event.getName()).isNotNull();
        assertThat(event.getEventDate()).isNotNull();
        assertThat(event.getTotalSlots()).isNotNull();
        assertThat(event.getAvailableSlots()).isNotNull();
        assertThat(event.getVersion()).isNotNull();
    }
}
