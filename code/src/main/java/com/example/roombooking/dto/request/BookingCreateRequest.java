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
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
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

    /** อุปกรณ์แต่ละชิ้นที่ขอจองเพิ่มเติม (map ตรงกับตาราง booking_equipment) */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EquipmentItemRequest {

        @NotNull(message = "กรุณาระบุอุปกรณ์")
        private Long equipmentId;

        @NotNull(message = "กรุณาระบุจำนวน")
        @Positive(message = "จำนวนอุปกรณ์ต้องมากกว่า 0")
        private Integer quantity;
    }
}
