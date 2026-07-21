package com.events.reservations.repository;

import com.events.reservations.entity.NotificationOutbox;
import com.events.reservations.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    List<NotificationOutbox> findTop50ByStatusOrderByCreatedAtAsc(NotificationStatus status);
}