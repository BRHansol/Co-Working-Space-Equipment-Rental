package com.example.roombooking.service.impl;

import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.repository.MeetingRoomRepository;
import com.example.roombooking.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomServiceImpl implements RoomService {

    private final MeetingRoomRepository roomRepository;

    @Autowired
    public RoomServiceImpl(MeetingRoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional
    public MeetingRoom createRoom(MeetingRoom room) {
        return roomRepository.save(room);
    }

    @Override
    public MeetingRoom getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("เชี่ย หาห้องไม่เจอว่ะ Room not found with id: " + id));
    }

    @Override
    public Page<MeetingRoom> getAllRooms(Pageable pageable) {
        return roomRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public MeetingRoom updateRoom(Long id, MeetingRoom roomDetails) {
        MeetingRoom existingRoom = getRoomById(id);
        
        existingRoom.setName(roomDetails.getName());
        existingRoom.setCapacity(roomDetails.getCapacity());
        existingRoom.setFloor(roomDetails.getFloor());
        existingRoom.setRoomType(roomDetails.getRoomType());
        existingRoom.setStatus(roomDetails.getStatus());
        
        return roomRepository.save(existingRoom);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        MeetingRoom existingRoom = getRoomById(id);
        roomRepository.delete(existingRoom);
    }
}