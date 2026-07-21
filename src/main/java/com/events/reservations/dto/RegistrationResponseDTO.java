package com.events.reservations.dto;

import com.events.reservations.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponseDTO {

    private Long id;
    private Long eventId;
    private String eventName;
    private String participantName;
    private String email;
    private RegistrationStatus status;
    private LocalDateTime createdAt;
}