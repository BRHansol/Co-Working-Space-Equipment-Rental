package com.example.roombooking.dto.request;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookingCreateRequestTest {
    @Test
    void rejectsPurposeBeyondDatabaseColumnLength() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            BookingCreateRequest request = validRequest();
            request.setPurpose("ก".repeat(255));
            assertTrue(validator.validate(request).isEmpty());
            request.setPurpose("ก".repeat(256));
            assertTrue(validator.validate(request).stream()
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("purpose")));
        }
    }

    @Test
    void validatesNullElementsAndNestedEquipmentQuantities() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            BookingCreateRequest request = validRequest();
            request.setEquipmentItems(Arrays.asList((BookingCreateRequest.EquipmentItemRequest) null));
            assertFalse(validator.validate(request).isEmpty());
            request.setEquipmentItems(List.of(new BookingCreateRequest.EquipmentItemRequest(7L, 0)));
            assertFalse(validator.validate(request).isEmpty());
            request.setEquipmentItems(List.of(new BookingCreateRequest.EquipmentItemRequest(7L, 1)));
            assertTrue(validator.validate(request).isEmpty());
        }
    }

    @Test
    void statusRequestRejectsMissingStatusAndOversizedReason() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            BookingStatusUpdateRequest request = new BookingStatusUpdateRequest();
            request.setReason("x".repeat(501));
            var violations = factory.getValidator().validate(request);
            assertEquals(2, violations.size());
        }
    }

    private BookingCreateRequest validRequest() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setRoomId(3L);
        request.setStartTime(LocalDateTime.of(2026, 10, 10, 9, 0));
        request.setEndTime(request.getStartTime().plusHours(1));
        return request;
    }
}
