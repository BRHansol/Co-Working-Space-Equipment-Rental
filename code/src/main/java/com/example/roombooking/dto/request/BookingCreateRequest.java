package com.example.roombooking.dto.request;

import java.time.LocalDateTime;
import java.util.List;



import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
public class BookingCreateRequest {
    private Long roomId;
    private Long bookingForUserId;

    @NotNull(message = "กรุณาระบุเวลาเริ่มต้น")
    private LocalDateTime startTime;
 
    @NotNull(message = "กรุณาระบุเวลาสิ้นสุด")
    private LocalDateTime endTime;
 
    /** ตรงกับ Booking.purpose */
    private String purpose;
 
    /** อุปกรณ์เพิ่มเติมที่ขอจองคู่กับห้อง (เป็น null หรือ list ว่างได้ถ้าไม่ขอ) */
    @Valid
    private List<EquipmentItemRequest> equipmentItems;
 
    /**
     * อุปกรณ์แต่ละชิ้นที่ขอจองเพิ่มเติม (map ตรงกับตาราง booking_equipment)
     */
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
