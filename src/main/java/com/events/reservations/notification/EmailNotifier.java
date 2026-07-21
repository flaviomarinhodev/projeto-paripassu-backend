package com.events.reservations.notification;

import com.events.reservations.entity.Registration;
import com.events.reservations.enums.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotifier implements RegistrationNotifier {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@event-reservations.local}")
    private String fromAddress;

    @Override
    public boolean supports(String channel) {
        return NotificationChannel.EMAIL.name().equals(channel);
    }

    @Override
    public void send(Registration registration) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(registration.getEmail());
        message.setSubject("Your reservation for " + registration.getEvent().getName() + " is confirmed");
        message.setText(buildBody(registration));

        mailSender.send(message);
        log.info("[EMAIL] Confirmation sent to {} for event {}",
                registration.getEmail(), registration.getEvent().getName());
    }

    private String buildBody(Registration registration) {
        return String.format(
                "Hello!%n%nYour reservation for the event '%s' was confirmed.%n" +
                        "We look forward to seeing you there.%n%nBest regards,%nEvent Reservations Team",
                registration.getEvent().getName()
        );
    }
}