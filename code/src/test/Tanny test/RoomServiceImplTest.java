package com.example.roombooking.service.impl;

import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.enums.RoomStatus;
import com.example.roombooking.domain.enums.RoomType;
import com.example.roombooking.dto.request.RoomCreateRequest;
import com.example.roombooking.dto.response.RoomResponse;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.RoomMapper;
import com.example.roombooking.repository.MeetingRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private MeetingRoomRepository meetingRoomRepository;

    private RoomServiceImpl roomService;

    @BeforeEach
    void setUp() {
        // ใช้ mapper ตัวจริง เพราะแค่แปลง field ไม่ต้อง mock
        roomService = new RoomServiceImpl(meetingRoomRepository, new RoomMapper());
    }

    private RoomCreateRequest buildRequest() {
        RoomCreateRequest request = new RoomCreateRequest();
        request.setName("Room A");
        request.setCapacity(10);
        request.setFloor("2");
        request.setRoomType(RoomType.VIP);
        request.setStatus(RoomStatus.AVAILABLE);
        return request;
    }

    private MeetingRoom buildRoom(Long id) {
        MeetingRoom room = new MeetingRoom();
        room.setId(id);
        room.setName("Room A");
        room.setCapacity(10);
        room.setFloor("2");
        room.setRoomType(RoomType.VIP);
        room.setStatus(RoomStatus.AVAILABLE);
        return room;
    }

    @Test
    void createRoom_savesAndReturnsResponse() {
        when(meetingRoomRepository.save(any(MeetingRoom.class))).thenAnswer(inv -> {
            MeetingRoom saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        RoomResponse response = roomService.createRoom(buildRequest());

        assertEquals(1L, response.getId());
        assertEquals("Room A", response.getName());
        assertEquals(RoomType.VIP, response.getRoomType());
        verify(meetingRoomRepository).save(any(MeetingRoom.class));
    }

    @Test
    void getRoomById_found_returnsResponse() {
        when(meetingRoomRepository.findById(1L)).thenReturn(Optional.of(buildRoom(1L)));

        RoomResponse response = roomService.getRoomById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Room A", response.getName());
    }

    @Test
    void getRoomById_notFound_throwsResourceNotFound() {
        when(meetingRoomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roomService.getRoomById(99L));
    }

    @Test
    void getAllRooms_returnsPageOfResponses() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<MeetingRoom> page = new PageImpl<>(List.of(buildRoom(1L), buildRoom(2L)), pageable, 2);
        when(meetingRoomRepository.findAll(pageable)).thenReturn(page);

        Page<RoomResponse> result = roomService.getAllRooms(pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
    }

    @Test
    void updateRoom_updatesFieldsAndSaves() {
        MeetingRoom existing = buildRoom(1L);
        when(meetingRoomRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(meetingRoomRepository.save(any(MeetingRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        RoomCreateRequest request = buildRequest();
        request.setName("Room B");
        request.setCapacity(20);
        request.setStatus(RoomStatus.MAINTENANCE);

        RoomResponse response = roomService.updateRoom(1L, request);

        assertEquals("Room B", response.getName());
        assertEquals(20, response.getCapacity());
        assertEquals(RoomStatus.MAINTENANCE, response.getStatus());
    }

    @Test
    void updateRoom_notFound_throwsResourceNotFound() {
        when(meetingRoomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> roomService.updateRoom(99L, buildRequest()));
        verify(meetingRoomRepository, never()).save(any(MeetingRoom.class));
    }

    @Test
    void deleteRoom_found_callsDelete() {
        MeetingRoom existing = buildRoom(1L);
        when(meetingRoomRepository.findById(1L)).thenReturn(Optional.of(existing));

        roomService.deleteRoom(1L);

        verify(meetingRoomRepository).delete(existing);
    }

    @Test
    void deleteRoom_notFound_throwsAndDoesNotDelete() {
        when(meetingRoomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roomService.deleteRoom(99L));
        verify(meetingRoomRepository, never()).delete(any(MeetingRoom.class));
    }
}
