package com.example.roombooking.dto.request;

import com.example.roombooking.domain.enums.BookingStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BookingStatusUpdateRequest {
     @NotNull(message = "กรุณาระบุสถานะที่ต้องการเปลี่ยน")
    private BookingStatus status;
 
    /** เหตุผลประกอบการเปลี่ยนสถานะ เช่น เหตุผลตอนปฏิเสธ/ยกเลิก (ไม่บังคับ) */
    @Size(max = 500, message = "เหตุผลต้องไม่เกิน 500 ตัวอักษร")
    private String reason;

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
