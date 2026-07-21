package com.events.reservations.exception;

public class SlotsUnavailableException extends RuntimeException {
    public SlotsUnavailableException(String message) {
        super(message);
    }
}