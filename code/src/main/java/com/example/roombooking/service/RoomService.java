package com.example.roombooking.service;

import com.example.roombooking.domain.entity.MeetingRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomService {
    MeetingRoom createRoom(MeetingRoom room);
    
    MeetingRoom getRoomById(Long id);
    
    Page<MeetingRoom> getAllRooms(Pageable pageable);
    
    MeetingRoom updateRoom(Long id, MeetingRoom roomDetails);
    
    void deleteRoom(Long id);
}