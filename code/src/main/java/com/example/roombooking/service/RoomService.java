package com.example.roombooking.service;

import com.example.roombooking.dto.request.RoomCreateRequest;
import com.example.roombooking.dto.response.RoomResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomService {
    RoomResponse createRoom(RoomCreateRequest request);
    RoomResponse getRoomById(Long id);
    Page<RoomResponse> getAllRooms(Pageable pageable);
    RoomResponse updateRoom(Long id, RoomCreateRequest request);
    void deleteRoom(Long id);
}