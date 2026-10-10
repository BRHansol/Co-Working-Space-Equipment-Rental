package com.example.roombooking.exception;

public class EquipmentNotAvailableException extends RuntimeException {

    public EquipmentNotAvailableException() {
        super();
    }

    public EquipmentNotAvailableException(String message) {
        super(message);
    }

    public EquipmentNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
