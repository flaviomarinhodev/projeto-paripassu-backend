package com.events.reservations.entity;

import com.events.reservations.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "registration",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_event_email", columnNames = {"event_id", "email"}),
                @UniqueConstraint(name = "uk_idempotency_key", columnNames = {"idempotency_key"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 150)
    private String participantName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}