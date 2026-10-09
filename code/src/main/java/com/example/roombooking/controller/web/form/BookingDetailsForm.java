package com.example.roombooking.controller.web.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Fields that a booking owner can change through the website. */
public class BookingDetailsForm {
    @NotNull(message = "กรุณาเลือกห้อง")
    @Positive(message = "กรุณาเลือกห้องที่ถูกต้อง")
    private Long roomId;

    @NotNull(message = "กรุณาระบุวันที่เริ่มต้น")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotNull(message = "กรุณาระบุเวลาเริ่มต้น")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "กรุณาระบุเวลาสิ้นสุด")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Size(max = 255, message = "วัตถุประสงค์ต้องไม่เกิน 255 ตัวอักษร")
    private String purpose;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public LocalDateTime getStartDateTime() {
        return date == null || startTime == null ? null : date.atTime(startTime);
    }

    public LocalDateTime getEndDateTime() {
        LocalDate lastDate = endDate == null ? date : endDate;
        return lastDate == null || endTime == null ? null : lastDate.atTime(endTime);
    }

    public BookingDetailsForm copy() {
        BookingDetailsForm copy = new BookingDetailsForm();
        copy.setRoomId(roomId);
        copy.setDate(date);
        copy.setEndDate(endDate);
        copy.setStartTime(startTime);
        copy.setEndTime(endTime);
        copy.setPurpose(purpose);
        return copy;
    }
}
