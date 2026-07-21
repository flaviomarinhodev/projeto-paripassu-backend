package com.events.reservations.notification;

import com.events.reservations.entity.Event;
import com.events.reservations.entity.Registration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotifierTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotifier emailNotifier;

    @Test
    void shouldSendConfirmationEmailToTheParticipant() {
        ReflectionTestUtils.setField(emailNotifier, "fromAddress", "no-reply@example.com");

        Event event = Event.builder()
                .id(1L)
                .name("Spring Boot Workshop")
                .eventDate(LocalDateTime.of(2026, 8, 15, 9, 0))
                .totalSlots(50)
                .availableSlots(50)
                .build();

        Registration registration = Registration.builder()
                .id(10L)
                .event(event)
                .email("participant@example.com")
                .build();

        emailNotifier.send(registration);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("no-reply@example.com", sentMessage.getFrom());
        assertEquals("participant@example.com", sentMessage.getTo()[0]);
        assertTrue(sentMessage.getText().contains("Spring Boot Workshop"));
        assertTrue(sentMessage.getSubject().contains("confirmed"));
    }
}
