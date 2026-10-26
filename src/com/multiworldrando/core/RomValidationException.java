package com.multiworldrando.core;

/** Thrown when the selected file is not a ROM this randomizer supports. */
public class RomValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    public RomValidationException(String message) {
        super(message);
    }
}
