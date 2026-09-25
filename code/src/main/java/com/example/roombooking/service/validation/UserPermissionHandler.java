package com.example.roombooking.service.validation;

import java.util.List;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserPermissionHandler {
    private final List<BookingValidationHandler> handlers; // เรียงตาม @Order โดย Spring แล้ว

    private BookingValidationHandler firstHandler;

    @PostConstruct
    void buildChain() {
        for (int i = 0; i < handlers.size() - 1; i++) {
            handlers.get(i).setNext(handlers.get(i + 1));
        }
        this.firstHandler = handlers.isEmpty() ? null : handlers.get(0);
    }

    public void validate(BookingValidationContext context) {
        if (firstHandler != null) {
            firstHandler.handle(context);
        }
    }
}
