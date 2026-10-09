package com.example.roombooking.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
public class BookingCreateRequest {
    /** ตั้งค่าโดย service ตอนแก้ไขการจองเดิม เพื่อไม่ให้ชนกับตัวเอง (ค่าที่ client ส่งมาจะถูกเขียนทับ) */
    private Long bookingId;

    @NotNull(message = "กรุณาระบุห้องประชุม")
    private Long roomId;

    private Long bookingForUserId;

    @NotNull(message = "กรุณาระบุเวลาเริ่มต้น")
    private LocalDateTime startTime;

    @NotNull(message = "กรุณาระบุเวลาสิ้นสุด")
    private LocalDateTime endTime;

    /** ตรงกับ Booking.purpose */
    @Size(max = 255, message = "วัตถุประสงค์ต้องไม่เกิน 255 ตัวอักษร")
    private String purpose;

    /** อุปกรณ์เพิ่มเติมที่ขอจองคู่กับห้อง (เป็น null หรือ list ว่างได้ถ้าไม่ขอ) */
    @Valid
    private List<@NotNull(message = "รายการอุปกรณ์ต้องไม่เป็น null") EquipmentItemRequest> equipmentItems;

    // Constructors
    public BookingCreateRequest() {
    }

    public BookingCreateRequest(Long bookingId, Long roomId, Long bookingForUserId,
                                LocalDateTime startTime, LocalDateTime endTime,
                                String purpose, List<EquipmentItemRequest> equipmentItems) {
        this.bookingId = bookingId;
        this.roomId = roomId;
        this.bookingForUserId = bookingForUserId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.equipmentItems = equipmentItems;
    }

    // Getters and Setters
    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getBookingForUserId() {
        return bookingForUserId;
    }

    public void setBookingForUserId(Long bookingForUserId) {
        this.bookingForUserId = bookingForUserId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public List<EquipmentItemRequest> getEquipmentItems() {
        return equipmentItems;
    }

    public void setEquipmentItems(List<EquipmentItemRequest> equipmentItems) {
        this.equipmentItems = equipmentItems;
    }

    /** อุปกรณ์แต่ละชิ้นที่ขอจองเพิ่มเติม (map ตรงกับตาราง booking_equipment) */
    @Getter
    @Setter
    @Builder
    public static class EquipmentItemRequest {

        @NotNull(message = "กรุณาระบุอุปกรณ์")
        private Long equipmentId;

        @NotNull(message = "กรุณาระบุจำนวน")
        @Positive(message = "จำนวนอุปกรณ์ต้องมากกว่า 0")
        private Integer quantity;

        public EquipmentItemRequest() {
        }

        public EquipmentItemRequest(Long equipmentId, Integer quantity) {
            this.equipmentId = equipmentId;
            this.quantity = quantity;
        }

        public Long getEquipmentId() {
            return equipmentId;
        }

        public void setEquipmentId(Long equipmentId) {
            this.equipmentId = equipmentId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
