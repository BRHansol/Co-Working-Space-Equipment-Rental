package com.example.roombooking.dto.response;

import com.example.roombooking.domain.enums.BookingStatus;

import java.time.LocalDateTime;

// ประวัติการเปลี่ยนสถานะ 1 ครั้ง ส่งเป็น DTO แทน entity เพื่อไม่ให้ Booking/User ทั้งก้อน (รวม password) หลุดออกไป
public class BookingStatusHistoryResponse {
    private Long id;
    private Long bookingId;
    private BookingStatus oldStatus;
    private BookingStatus newStatus;
    private Long changedById;
    private String changedBy;
    private LocalDateTime changedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public BookingStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(BookingStatus oldStatus) { this.oldStatus = oldStatus; }
    public BookingStatus getNewStatus() { return newStatus; }
    public void setNewStatus(BookingStatus newStatus) { this.newStatus = newStatus; }
    public Long getChangedById() { return changedById; }
    public void setChangedById(Long changedById) { this.changedById = changedById; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
