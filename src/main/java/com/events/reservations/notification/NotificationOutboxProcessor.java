package com.events.reservations.notification;

import com.events.reservations.entity.NotificationOutbox;
import com.events.reservations.enums.NotificationStatus;
import com.events.reservations.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Worker that processes the notification outbox asynchronously,
 * ensuring each notification is sent exactly once,
 * even though the registration has already been committed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxProcessor {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final List<RegistrationNotifier> notifiers;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processPending() {
        List<NotificationOutbox> pending =
                notificationOutboxRepository.findTop50ByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING);

        for (NotificationOutbox notification : pending) {
            try {
                notifiers.stream()
                        .filter(n -> n.supports(notification.getChannel().name()))
                        .findFirst()
                        .ifPresent(notifier -> notifier.send(notification.getRegistration()));

                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } catch (Exception ex) {
                notification.setAttempts(notification.getAttempts() + 1);
                notification.setStatus(NotificationStatus.FAILED);
                log.error("Failed to send notification id={}", notification.getId(), ex);
            }
            notificationOutboxRepository.save(notification);
        }
    }
}