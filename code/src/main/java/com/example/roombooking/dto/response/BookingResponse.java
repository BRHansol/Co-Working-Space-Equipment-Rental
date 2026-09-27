package com.example.roombooking.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.example.roombooking.domain.enums.BookingStatus;
import lombok.Builder;

@Builder
public class BookingResponse {
     private Long id;
 
    private Long roomId;
    private String roomName;
 
    private Long userId;
    private String username;
 
    private LocalDateTime startTime;
    private LocalDateTime endTime;
 
    private BookingStatus status;
    private String purpose;
 
    public BookingResponse(Long id, Long roomId, String roomName, Long userId, String username, LocalDateTime startTime,
            LocalDateTime endTime, BookingStatus status, String purpose, List<EquipmentItem> equipmentItems,
            LocalDateTime createdAt) {
        this.id = id;
        this.roomId = roomId;
        this.roomName = roomName;
        this.userId = userId;
        this.username = username;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.purpose = purpose;
        this.equipmentItems = equipmentItems;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public List<EquipmentItem> getEquipmentItems() {
        return equipmentItems;
    }

    public void setEquipmentItems(List<EquipmentItem> equipmentItems) {
        this.equipmentItems = equipmentItems;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    private List<EquipmentItem> equipmentItems;
 
    private LocalDateTime createdAt;

    @Builder
    public static class EquipmentItem {
        private Long equipmentId;
        private String equipmentName;
        private Integer quantity;
        public EquipmentItem(Long equipmentId, String equipmentName, Integer quantity) {
            this.equipmentId = equipmentId;
            this.equipmentName = equipmentName;
            this.quantity = quantity;
        }
        public Long getEquipmentId() {
            return equipmentId;
        }
        public void setEquipmentId(Long equipmentId) {
            this.equipmentId = equipmentId;
        }
        public String getEquipmentName() {
            return equipmentName;
        }
        public void setEquipmentName(String equipmentName) {
            this.equipmentName = equipmentName;
        }
        public Integer getQuantity() {
            return quantity;
        }
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
        
    }
}
