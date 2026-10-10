package com.example.roombooking.dto.request;

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
 * Boundary Value Testing ของ EquipmentCreateRequest (Week 3)
 *
 *   X1 = ความยาวของ name   ช่วงที่ถูกต้อง [1, 100]          (@NotBlank, @Size(max = 100))
 *   X2 = totalQuantity     ช่วงที่ถูกต้อง [0, 2147483647]   (@NotNull, @PositiveOrZero)
 *
 * ค่าที่ใช้     min-  min  min+  nom  max-          max
 *   X1          0     1    2     50   99            100    (max+ = 101)
 *   X2          -1    0    1     5    2147483646    2147483647
 */
@DisplayName("EquipmentCreateRequest: Boundary Value Testing")
class EquipmentCreateRequestBoundaryTest {

    static final int NAME_MIN_MINUS = 0, NAME_MIN = 1, NAME_MIN_PLUS = 2, NAME_NOM = 50,
            NAME_MAX_MINUS = 99, NAME_MAX = 100, NAME_MAX_PLUS = 101;
    static final int QTY_MIN_MINUS = -1, QTY_MIN = 0, QTY_MIN_PLUS = 1, QTY_NOM = 5,
            QTY_MAX_MINUS = Integer.MAX_VALUE - 1, QTY_MAX = Integer.MAX_VALUE;

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

    static Arguments row(int nameLength, int quantity) {
        boolean valid = nameLength >= NAME_MIN && nameLength <= NAME_MAX && quantity >= QTY_MIN;
        return Arguments.of(nameLength, quantity, valid);
    }

    private void assertValidity(int nameLength, int quantity, boolean expectedValid) {
        EquipmentCreateRequest request = new EquipmentCreateRequest();
        request.setName("P".repeat(nameLength));
        request.setTotalQuantity(quantity);
        request.setCategory("Display");

        Set<ConstraintViolation<EquipmentCreateRequest>> violations = validator.validate(request);

        assertEquals(expectedValid, violations.isEmpty(),
                () -> "nameLength=" + nameLength + ", totalQuantity=" + quantity + " -> " + violations);
    }

    // Normal BVA: 4n + 1 = 9
    static Stream<Arguments> normalBoundaryCases() {
        List<Arguments> rows = new ArrayList<>();
        rows.add(row(NAME_NOM, QTY_NOM));
        for (int n : new int[]{NAME_MIN, NAME_MIN_PLUS, NAME_MAX_MINUS, NAME_MAX}) rows.add(row(n, QTY_NOM));
        for (int q : new int[]{QTY_MIN, QTY_MIN_PLUS, QTY_MAX_MINUS, QTY_MAX}) rows.add(row(NAME_NOM, q));
        return rows.stream();
    }

    @ParameterizedTest(name = "[{index}] name length={0}, totalQuantity={1} -> valid={2}")
    @MethodSource("normalBoundaryCases")
    @DisplayName("Normal boundary value analysis (4n+1 = 9)")
    void normalBoundaryValueAnalysis(int nameLength, int quantity, boolean expectedValid) {
        assertValidity(nameLength, quantity, expectedValid);
    }

    // Robustness: 6n + 1 = 13 (totalQuantity max+ เกิน Integer จึงไม่นับ = 12)
    static Stream<Arguments> robustnessCases() {
        List<Arguments> rows = new ArrayList<>(normalBoundaryCases().toList());
        rows.add(row(NAME_MIN_MINUS, QTY_NOM));
        rows.add(row(NAME_MAX_PLUS, QTY_NOM));
        rows.add(row(NAME_NOM, QTY_MIN_MINUS));
        return rows.stream();
    }

    @ParameterizedTest(name = "[{index}] name length={0}, totalQuantity={1} -> valid={2}")
    @MethodSource("robustnessCases")
    @DisplayName("Robustness testing (6n+1 = 13, ทดสอบได้ 12)")
    void robustnessTesting(int nameLength, int quantity, boolean expectedValid) {
        assertValidity(nameLength, quantity, expectedValid);
    }
}
