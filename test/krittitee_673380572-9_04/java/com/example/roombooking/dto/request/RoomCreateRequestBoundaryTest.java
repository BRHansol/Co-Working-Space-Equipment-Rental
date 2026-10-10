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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Boundary Value Testing ของ RoomCreateRequest (Week 3): normal, robustness, worst-case, robust worst-case
 *
 * ตัวแปรที่ทดสอบ 2 ตัว (n = 2)
 *   X1 = ความยาวของ name   ช่วงที่ถูกต้อง [1, 100]   (@NotBlank, @Size(max = 100))
 *   X2 = capacity          ช่วงที่ถูกต้อง [1, 2147483647] (@NotNull, @Positive, ชนิด Integer)
 *
 * ค่าที่ใช้     min-  min  min+  nom  max-          max           max+
 *   X1          0     1    2     50   99            100           101
 *   X2          0     1    2     10   2147483646    2147483647    2147483648 (เกินช่วง Integer)
 *
 * X2 max+ ใส่ลง Integer ไม่ได้ จึงทดสอบที่ระดับ API แทน
 * (RoomControllerTest.createRoom_capacityAboveIntegerMax_returns400AndDoesNotCallService)
 */
@DisplayName("RoomCreateRequest: Boundary Value Testing")
class RoomCreateRequestBoundaryTest {

    static final int NAME_MIN_MINUS = 0, NAME_MIN = 1, NAME_MIN_PLUS = 2, NAME_NOM = 50,
            NAME_MAX_MINUS = 99, NAME_MAX = 100, NAME_MAX_PLUS = 101;
    static final int CAP_MIN_MINUS = 0, CAP_MIN = 1, CAP_MIN_PLUS = 2, CAP_NOM = 10,
            CAP_MAX_MINUS = Integer.MAX_VALUE - 1, CAP_MAX = Integer.MAX_VALUE;

    static final int[] NAME_5 = {NAME_MIN, NAME_MIN_PLUS, NAME_NOM, NAME_MAX_MINUS, NAME_MAX};
    static final int[] CAP_5 = {CAP_MIN, CAP_MIN_PLUS, CAP_NOM, CAP_MAX_MINUS, CAP_MAX};
    static final int[] NAME_7 = {NAME_MIN_MINUS, NAME_MIN, NAME_MIN_PLUS, NAME_NOM, NAME_MAX_MINUS, NAME_MAX, NAME_MAX_PLUS};
    static final int[] CAP_6 = {CAP_MIN_MINUS, CAP_MIN, CAP_MIN_PLUS, CAP_NOM, CAP_MAX_MINUS, CAP_MAX};

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

    // oracle มาจาก spec: ถูกต้องเมื่อทั้งสองตัวแปรอยู่ในช่วง
    static boolean expectedValid(int nameLength, int capacity) {
        return nameLength >= NAME_MIN && nameLength <= NAME_MAX && capacity >= CAP_MIN;
    }

    static Arguments row(int nameLength, int capacity) {
        return Arguments.of(nameLength, capacity, expectedValid(nameLength, capacity));
    }

    private void assertValidity(int nameLength, int capacity, boolean expectedValid) {
        RoomCreateRequest request = new RoomCreateRequest();
        request.setName("A".repeat(nameLength));
        request.setCapacity(capacity);
        request.setFloor("2");
        request.setRoomType(RoomType.STANDARD);
        request.setStatus(RoomStatus.AVAILABLE);

        Set<ConstraintViolation<RoomCreateRequest>> violations = validator.validate(request);

        assertEquals(expectedValid, violations.isEmpty(),
                () -> "nameLength=" + nameLength + ", capacity=" + capacity + " -> " + violations);
    }

    // ---------- 1) Normal BVA: 4n + 1 = 9 cases (single fault, อีกตัวอยู่ค่า nominal) ----------
    static Stream<Arguments> normalBoundaryCases() {
        List<Arguments> rows = new ArrayList<>();
        rows.add(row(NAME_NOM, CAP_NOM));
        for (int n : new int[]{NAME_MIN, NAME_MIN_PLUS, NAME_MAX_MINUS, NAME_MAX}) rows.add(row(n, CAP_NOM));
        for (int c : new int[]{CAP_MIN, CAP_MIN_PLUS, CAP_MAX_MINUS, CAP_MAX}) rows.add(row(NAME_NOM, c));
        return rows.stream();
    }

    @ParameterizedTest(name = "[{index}] name length={0}, capacity={1} -> valid={2}")
    @MethodSource("normalBoundaryCases")
    @DisplayName("Normal boundary value analysis (4n+1 = 9)")
    void normalBoundaryValueAnalysis(int nameLength, int capacity, boolean expectedValid) {
        assertValidity(nameLength, capacity, expectedValid);
    }

    // ---------- 2) Robustness: 6n + 1 = 13 cases (12 ที่ระดับ DTO + 1 ที่ระดับ API) ----------
    static Stream<Arguments> robustnessCases() {
        List<Arguments> rows = new ArrayList<>(normalBoundaryCases().toList());
        rows.add(row(NAME_MIN_MINUS, CAP_NOM));
        rows.add(row(NAME_MAX_PLUS, CAP_NOM));
        rows.add(row(NAME_NOM, CAP_MIN_MINUS));
        return rows.stream();
    }

    @ParameterizedTest(name = "[{index}] name length={0}, capacity={1} -> valid={2}")
    @MethodSource("robustnessCases")
    @DisplayName("Robustness testing (6n+1 = 13, capacity max+ ทดสอบที่ API)")
    void robustnessTesting(int nameLength, int capacity, boolean expectedValid) {
        assertValidity(nameLength, capacity, expectedValid);
    }

    // ---------- 3) Worst-case: 5^n = 25 cases (multiple fault) ----------
    static Stream<Arguments> worstCaseCases() {
        List<Arguments> rows = new ArrayList<>();
        for (int n : NAME_5) for (int c : CAP_5) rows.add(row(n, c));
        return rows.stream();
    }

    @ParameterizedTest(name = "[{index}] name length={0}, capacity={1} -> valid={2}")
    @MethodSource("worstCaseCases")
    @DisplayName("Worst-case testing (5^n = 25)")
    void worstCaseTesting(int nameLength, int capacity, boolean expectedValid) {
        assertValidity(nameLength, capacity, expectedValid);
    }

    // ---------- 4) Robust worst-case: 7^n = 49 cases (42 ที่ระดับ DTO, 7 ที่มี capacity max+ ทำไม่ได้ใน Integer) ----------
    static Stream<Arguments> robustWorstCaseCases() {
        List<Arguments> rows = new ArrayList<>();
        for (int n : NAME_7) for (int c : CAP_6) rows.add(row(n, c));
        return rows.stream();
    }

    @ParameterizedTest(name = "[{index}] name length={0}, capacity={1} -> valid={2}")
    @MethodSource("robustWorstCaseCases")
    @DisplayName("Robust worst-case testing (7^n = 49, ทดสอบได้ 42)")
    void robustWorstCaseTesting(int nameLength, int capacity, boolean expectedValid) {
        assertValidity(nameLength, capacity, expectedValid);
    }
}
