package com.example.roombooking.dto.request;

import com.example.roombooking.domain.enums.BookingStatus;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BookingCreateRequestTest {
    @Test
    void missingBookingDetailsReportEachRequiredField() {
        assertEquals(Set.of("roomId", "startTime", "endTime"), violationPaths(new BookingCreateRequest()));
    }

    @Test
    void bookingWithoutPurposeOrEquipmentIsValid() {
        BookingCreateRequest request = validRequest();
        assertTrue(violationPaths(request).isEmpty());

        request.setPurpose("");
        request.setEquipmentItems(List.of());
        assertTrue(violationPaths(request).isEmpty());
    }

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

    @ParameterizedTest(name = "equipmentId={0}, quantity={1} rejects {2}")
    @MethodSource("invalidEquipmentItems")
    void rejectsInvalidNestedEquipmentField(Long equipmentId, Integer quantity, String field) {
        BookingCreateRequest request = validRequest();
        request.setEquipmentItems(List.of(new BookingCreateRequest.EquipmentItemRequest(equipmentId, quantity)));

        assertEquals(Set.of("equipmentItems[0]." + field), violationPaths(request));
    }

    private static Stream<Arguments> invalidEquipmentItems() {
        return Stream.of(
                Arguments.of(null, 1, "equipmentId"),
                Arguments.of(7L, null, "quantity"),
                Arguments.of(7L, -1, "quantity"),
                Arguments.of(7L, Integer.MIN_VALUE, "quantity")
        );
    }

    @Test
    void validationReportsTheInvalidItemIndexWithoutRejectingItsValidSibling() {
        BookingCreateRequest request = validRequest();
        request.setEquipmentItems(List.of(
                new BookingCreateRequest.EquipmentItemRequest(7L, 1),
                new BookingCreateRequest.EquipmentItemRequest(null, -1)));

        assertEquals(Set.of("equipmentItems[1].equipmentId", "equipmentItems[1].quantity"),
                violationPaths(request));
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

    @Test
    void statusRequestAcceptsMaximumLengthAndOptionalReason() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            BookingStatusUpdateRequest request = new BookingStatusUpdateRequest();
            request.setStatus(BookingStatus.REJECTED);
            request.setReason("ก".repeat(500));
            assertTrue(validator.validate(request).isEmpty());

            request.setReason(null);
            assertTrue(validator.validate(request).isEmpty());
        }
    }

    private Set<String> violationPaths(BookingCreateRequest request) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator().validate(request).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
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
