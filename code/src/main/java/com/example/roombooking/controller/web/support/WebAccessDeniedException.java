package com.example.roombooking.controller.web.support;

public class WebAccessDeniedException extends RuntimeException {
    public WebAccessDeniedException(String message) {
        super(message);
    }
}
