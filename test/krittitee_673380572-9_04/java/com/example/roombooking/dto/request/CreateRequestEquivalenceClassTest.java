package com.example.roombooking.dto.request;

import com.example.roombooking.domain.enums.RoomStatus;
import com.example.roombooking.domain.enums.RoomType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Equivalence Class Testing (Week 4) แบบ single fault:
 * แต่ละ invalid case ผิดแค่ 1 class เพื่อยืนยันว่า validation ของ field นั้นเป็นตัวจับได้จริง
 * เลข EC ตรงกับ sheet "Equivalence Classes" ในไฟล์ Test Report
 */
@DisplayName("Create request: Equivalence Class Testing (single fault)")
class CreateRequestEquivalenceClassTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    static RoomCreateRequest validRoom() {
        RoomCreateRequest request = new RoomCreateRequest();
        request.setName("Room A");
        request.setCapacity(10);
        request.setFloor("2");
        request.setRoomType(RoomType.STANDARD);
        request.setStatus(RoomStatus.AVAILABLE);
        return request;
    }

    static EquipmentCreateRequest validEquipment() {
        EquipmentCreateRequest request = new EquipmentCreateRequest();
        request.setName("Projector");
        request.setTotalQuantity(5);
        request.setCategory("Display");
        return request;
    }

    private static <T> void assertOnlyFieldInvalid(T request, String expectedField) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        assertEquals(1, violations.size(), () -> "ต้องผิดแค่ field เดียว แต่ได้ " + violations);
        assertEquals(expectedField, violations.iterator().next().getPropertyPath().toString());
    }

    static Stream<Arguments> roomValidClasses() {
        return Stream.of(
                Arguments.of("EC1, EC4, EC7, EC9, EC11: ทุก field ถูกต้อง", (Consumer<RoomCreateRequest>) r -> { }),
                Arguments.of("EC7: floor = null (ไม่บังคับ)", (Consumer<RoomCreateRequest>) r -> r.setFloor(null)),
                Arguments.of("EC7: floor ยาว 20 ตัว", (Consumer<RoomCreateRequest>) r -> r.setFloor("F".repeat(20))),
                Arguments.of("EC9: roomType = VIP", (Consumer<RoomCreateRequest>) r -> r.setRoomType(RoomType.VIP)),
                Arguments.of("EC11: status = MAINTENANCE", (Consumer<RoomCreateRequest>) r -> r.setStatus(RoomStatus.MAINTENANCE)));
    }

    static Stream<Arguments> roomInvalidClasses() {
        return Stream.of(
                Arguments.of("EC2: name ว่าง", "name", (Consumer<RoomCreateRequest>) r -> r.setName("")),
                Arguments.of("EC2: name มีแต่ช่องว่าง", "name", (Consumer<RoomCreateRequest>) r -> r.setName("   ")),
                Arguments.of("EC2: name = null", "name", (Consumer<RoomCreateRequest>) r -> r.setName(null)),
                Arguments.of("EC3: name ยาว 101 ตัว", "name", (Consumer<RoomCreateRequest>) r -> r.setName("A".repeat(101))),
                Arguments.of("EC5: capacity = 0", "capacity", (Consumer<RoomCreateRequest>) r -> r.setCapacity(0)),
                Arguments.of("EC5: capacity = -1", "capacity", (Consumer<RoomCreateRequest>) r -> r.setCapacity(-1)),
                Arguments.of("EC6: capacity = null", "capacity", (Consumer<RoomCreateRequest>) r -> r.setCapacity(null)),
                Arguments.of("EC8: floor ยาว 21 ตัว", "floor", (Consumer<RoomCreateRequest>) r -> r.setFloor("F".repeat(21))),
                Arguments.of("EC10: roomType = null", "roomType", (Consumer<RoomCreateRequest>) r -> r.setRoomType(null)),
                Arguments.of("EC12: status = null", "status", (Consumer<RoomCreateRequest>) r -> r.setStatus(null)));
    }

    @Nested
    @DisplayName("RoomCreateRequest")
    class Room {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.example.roombooking.dto.request.CreateRequestEquivalenceClassTest#roomValidClasses")
        @DisplayName("Valid equivalence classes ต้องผ่าน validation")
        void validClass_hasNoViolation(String ec, Consumer<RoomCreateRequest> change) {
            RoomCreateRequest request = validRoom();
            change.accept(request);
            assertTrue(validator.validate(request).isEmpty());
        }

        @ParameterizedTest(name = "{0} -> field {1} ไม่ผ่าน")
        @MethodSource("com.example.roombooking.dto.request.CreateRequestEquivalenceClassTest#roomInvalidClasses")
        @DisplayName("Invalid equivalence classes ต้องผิดเฉพาะ field ที่ทดสอบ")
        void invalidClass_onlyThatFieldFails(String ec, String field, Consumer<RoomCreateRequest> change) {
            RoomCreateRequest request = validRoom();
            change.accept(request);
            assertOnlyFieldInvalid(request, field);
        }
    }

    static Stream<Arguments> equipmentValidClasses() {
        return Stream.of(
                Arguments.of("EC13, EC16, EC19: ทุก field ถูกต้อง", (Consumer<EquipmentCreateRequest>) e -> { }),
                Arguments.of("EC16: totalQuantity = 0", (Consumer<EquipmentCreateRequest>) e -> e.setTotalQuantity(0)),
                Arguments.of("EC19: category = null (ไม่บังคับ)", (Consumer<EquipmentCreateRequest>) e -> e.setCategory(null)),
                Arguments.of("EC19: category ยาว 50 ตัว", (Consumer<EquipmentCreateRequest>) e -> e.setCategory("C".repeat(50))));
    }

    static Stream<Arguments> equipmentInvalidClasses() {
        return Stream.of(
                Arguments.of("EC14: name ว่าง", "name", (Consumer<EquipmentCreateRequest>) e -> e.setName("")),
                Arguments.of("EC14: name มีแต่ช่องว่าง", "name", (Consumer<EquipmentCreateRequest>) e -> e.setName("   ")),
                Arguments.of("EC15: name ยาว 101 ตัว", "name", (Consumer<EquipmentCreateRequest>) e -> e.setName("P".repeat(101))),
                Arguments.of("EC17: totalQuantity = -1", "totalQuantity", (Consumer<EquipmentCreateRequest>) e -> e.setTotalQuantity(-1)),
                Arguments.of("EC18: totalQuantity = null", "totalQuantity", (Consumer<EquipmentCreateRequest>) e -> e.setTotalQuantity(null)),
                Arguments.of("EC20: category ยาว 51 ตัว", "category", (Consumer<EquipmentCreateRequest>) e -> e.setCategory("C".repeat(51))));
    }

    @Nested
    @DisplayName("EquipmentCreateRequest")
    class Equipment {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.example.roombooking.dto.request.CreateRequestEquivalenceClassTest#equipmentValidClasses")
        @DisplayName("Valid equivalence classes ต้องผ่าน validation")
        void validClass_hasNoViolation(String ec, Consumer<EquipmentCreateRequest> change) {
            EquipmentCreateRequest request = validEquipment();
            change.accept(request);
            assertTrue(validator.validate(request).isEmpty());
        }

        @ParameterizedTest(name = "{0} -> field {1} ไม่ผ่าน")
        @MethodSource("com.example.roombooking.dto.request.CreateRequestEquivalenceClassTest#equipmentInvalidClasses")
        @DisplayName("Invalid equivalence classes ต้องผิดเฉพาะ field ที่ทดสอบ")
        void invalidClass_onlyThatFieldFails(String ec, String field, Consumer<EquipmentCreateRequest> change) {
            EquipmentCreateRequest request = validEquipment();
            change.accept(request);
            assertOnlyFieldInvalid(request, field);
        }
    }
}
