package com.example.roombooking.service.impl;

import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.dto.request.RoomCreateRequest;
import com.example.roombooking.dto.response.RoomResponse;
import com.example.roombooking.mapper.RoomMapper;
import com.example.roombooking.repository.MeetingRoomRepository;
import com.example.roombooking.service.RoomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomServiceImpl implements RoomService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final RoomMapper roomMapper; // <--- เพิ่ม Mapper

    public RoomServiceImpl(MeetingRoomRepository meetingRoomRepository, RoomMapper roomMapper) {
        this.meetingRoomRepository = meetingRoomRepository;
        this.roomMapper = roomMapper;
    }

    @Override
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request) {
        MeetingRoom room = roomMapper.toEntity(request);
        MeetingRoom savedRoom = meetingRoomRepository.save(room);
        return roomMapper.toResponse(savedRoom);
    }

    @Override
    public RoomResponse getRoomById(Long id) {
        MeetingRoom room = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting room not found with id: " + id));
        return roomMapper.toResponse(room);
    }

    @Override
    public Page<RoomResponse> getAllRooms(Pageable pageable) {
        return meetingRoomRepository.findAll(pageable)
                .map(roomMapper::toResponse); // ใช้ Mapper แปลงทีละตัวใน Page
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Long id, RoomCreateRequest request) {
        MeetingRoom existingRoom = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting room not found with id: " + id));

        existingRoom.setName(request.getName());
        existingRoom.setCapacity(request.getCapacity());
        existingRoom.setFloor(request.getFloor());
        existingRoom.setRoomType(request.getRoomType());
        existingRoom.setStatus(request.getStatus());

        MeetingRoom updatedRoom = meetingRoomRepository.save(existingRoom);
        return roomMapper.toResponse(updatedRoom);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        MeetingRoom existingRoom = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting room not found with id: " + id));
        meetingRoomRepository.delete(existingRoom);
    }
}