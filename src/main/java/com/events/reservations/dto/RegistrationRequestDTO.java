package com.events.reservations.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationRequestDTO {

    @NotNull(message = "eventId is required")
    private Long eventId;

    @NotBlank(message = "participantName is required")
    private String participantName;

    @NotBlank(message = "email is required")
    @Email(message = "invalid email")
    private String email;
}