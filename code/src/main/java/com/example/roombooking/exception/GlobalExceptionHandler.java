package com.example.roombooking.exception;

import com.example.roombooking.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;


@RestControllerAdvice(basePackages = "com.example.roombooking.controller.api")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404 - หา resource ที่ขอไม่เจอ (เช่น room/booking/user id ไม่มีจริง)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
                                                                 HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    // 409 - ห้องไม่ว่าง / ถูกจองทับซ้อนช่วงเวลา
    @ExceptionHandler(RoomNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleRoomNotAvailable(RoomNotAvailableException ex,
                                                                 HttpServletRequest request) {
        log.warn("Room not available: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // 409 - อุปกรณ์ไม่พอ/ไม่ว่าง
    @ExceptionHandler(EquipmentNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleEquipmentNotAvailable(EquipmentNotAvailableException ex,
                                                                      HttpServletRequest request) {
        log.warn("Equipment not available: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // 409 - เปลี่ยนสถานะไม่ได้ตาม State Pattern (เช่น approve booking ที่ CANCELLED ไปแล้ว)
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransition(InvalidStateTransitionException ex,
                                                                       HttpServletRequest request) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // 409 - ข้อมูลชนกันทั่วไป (เช่น username/email ซ้ำ)
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        log.warn("Conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // 403 - ผู้ใช้ไม่มีสิทธิ์ทำรายการนี้ (เช่นไม่ใช่เจ้าของ booking / ไม่ใช่ admin)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex,
                                                          HttpServletRequest request) {
        log.warn("Forbidden: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    // 400 - request body ไม่ผ่าน @Valid (เช่น field required ใน DTO)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());
        log.warn("Validation failed: {}", details);
        return buildResponse(HttpStatus.BAD_REQUEST, "ข้อมูลที่ส่งมาไม่ถูกต้อง", request, details);
    }

    // 400 - body อ่านไม่ได้ (JSON พัง, enum ไม่มีจริง, วันที่ผิดรูปแบบ)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
                                                            HttpServletRequest request) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "รูปแบบข้อมูลใน request body ไม่ถูกต้อง", request, null);
    }

    // 400 - path variable / query param แปลงชนิดไม่ได้ (เช่น /bookings/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                             HttpServletRequest request) {
        log.warn("Type mismatch: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "ค่าของ '" + ex.getName() + "' ไม่ถูกต้อง: " + ex.getValue(), request, null);
    }

    // 400 - argument ผิดรูปแบบทั่วไป (เช่น enum ไม่ตรง, ค่าที่ไม่สมเหตุสมผล)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    // 500 - ตัวดักจับสุดท้าย กัน stack trace หลุดออกไปหา client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        // exception ของ Spring MVC เอง (404 ไม่มี path, 405 method ผิด, ขาด header ฯลฯ) รู้ status ของตัวเองอยู่แล้ว
        // ไม่งั้นจะโดนตัวดักนี้แปลงเป็น 500 หมด
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode code = springError.getStatusCode();
            HttpStatus status = HttpStatus.resolve(code.value());
            log.warn("Request error {}: {}", code.value(), ex.getMessage());
            return buildResponse(status != null ? status : HttpStatus.BAD_REQUEST,
                    springError.getBody().getDetail(), request, null);
        }
        log.error("Unhandled exception", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "เกิดข้อผิดพลาดที่ไม่คาดคิด กรุณาลองใหม่อีกครั้ง", request, null);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message,
                                                         HttpServletRequest request, List<String> details) {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                details
        );
        return ResponseEntity.status(status).body(body);
    }
}
