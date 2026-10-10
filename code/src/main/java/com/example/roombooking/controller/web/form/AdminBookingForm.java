package com.example.roombooking.controller.web.form;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.*;
import java.util.*;
@Data
public class AdminBookingForm {
    @NotNull(message="กรุณาเลือกผู้ใช้") private Long userId;
    @NotNull(message="กรุณาเลือกห้อง") private Long roomId;
    @NotNull(message="กรุณาระบุวันที่") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate date;
    @NotNull(message="กรุณาระบุเวลาเริ่มต้น") @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) private LocalTime startTime = LocalTime.of(9,0);
    @NotNull(message="กรุณาระบุเวลาสิ้นสุด") @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) private LocalTime endTime = LocalTime.of(11,0);
    @Size(max=255,message="วัตถุประสงค์ต้องไม่เกิน 255 ตัวอักษร") private String purpose;
    @Valid @Size(max=100) private List<Item> equipmentItems = new ArrayList<>();
    @Data public static class Item {
        @NotNull private Long equipmentId;
        @NotNull @Min(0) private Integer quantity = 0;
    }
}
