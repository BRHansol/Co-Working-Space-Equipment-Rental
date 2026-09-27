package com.example.roombooking.dto.request;

import com.example.roombooking.domain.enums.RoomStatus;
import com.example.roombooking.domain.enums.RoomType;

public class RoomCreateRequest {
    private String name;
    private Integer capacity;
    private String floor;
    private RoomType roomType;
    private RoomStatus status;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }
    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
}