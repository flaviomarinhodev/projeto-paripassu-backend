package com.events.reservations.notification;

import com.events.reservations.entity.Registration;

public interface RegistrationNotifier {

    boolean supports(String channel);

    void send(Registration registration);
}