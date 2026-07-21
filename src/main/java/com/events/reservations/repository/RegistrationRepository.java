package com.events.reservations.repository;

import com.events.reservations.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findByIdempotencyKey(String idempotencyKey);

    boolean existsByEventIdAndEmail(Long eventId, String email);
}