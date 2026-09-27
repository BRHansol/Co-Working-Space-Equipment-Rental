package com.example.roombooking.mapper;

import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.dto.request.RoomCreateRequest;
import com.example.roombooking.dto.response.RoomResponse;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

    // แปลงจาก Request DTO -> Entity (ใช้ตอน Create)
    public MeetingRoom toEntity(RoomCreateRequest request) {
        MeetingRoom room = new MeetingRoom();
        room.setName(request.getName());
        room.setCapacity(request.getCapacity());
        room.setFloor(request.getFloor());
        room.setRoomType(request.getRoomType());
        room.setStatus(request.getStatus());
        return room;
    }

    // แปลงจาก Entity -> Response DTO (ใช้ตอนส่งกลับ)
    public RoomResponse toResponse(MeetingRoom entity) {
        RoomResponse response = new RoomResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setCapacity(entity.getCapacity());
        response.setFloor(entity.getFloor());
        response.setRoomType(entity.getRoomType());
        response.setStatus(entity.getStatus());
        return response;
    }
}