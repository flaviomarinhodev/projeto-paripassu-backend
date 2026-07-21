package com.events.reservations.repository;

import com.events.reservations.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Modifying
    @Query("""
           UPDATE Event e
           SET e.availableSlots = e.availableSlots - 1
           WHERE e.id = :eventId
           AND e.availableSlots > 0
           """)
    int decrementSlotAtomically(@Param("eventId") Long eventId);
}