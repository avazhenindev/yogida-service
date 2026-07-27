package com.yogida.meditation.exception;

/**
 * Thrown when a breathing exercise or phase is not found.
 */
public class BreathingNotFoundException extends RuntimeException {

    public BreathingNotFoundException(Long id) {
        super("Breathing exercise not found with id: " + id);
    }

    public BreathingNotFoundException(String message) {
        super(message);
    }
}
