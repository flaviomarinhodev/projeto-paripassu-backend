package com.events.reservations.notification;

import com.events.reservations.entity.Registration;
import com.events.reservations.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotifier implements RegistrationNotifier {

    @Override
    public boolean supports(String channel) {
        return NotificationChannel.EMAIL.name().equals(channel);
    }

    @Override
    public void send(Registration registration) {
        // Simulated email sending — in production, integrate with a real provider (SMTP, SES, SendGrid, etc.)
        log.info("[EMAIL] Sending confirmation to {} - Event: {}",
                registration.getEmail(), registration.getEvent().getName());
    }
}