package com.example.roombooking.controller;

import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.service.RoomService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<MeetingRoom> createRoom(@RequestBody MeetingRoom meetingRoom) {
        return new ResponseEntity<>(roomService.createRoom(meetingRoom), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeetingRoom> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @GetMapping
    public ResponseEntity<Page<MeetingRoom>> getAllRooms(Pageable pageable) {
        return ResponseEntity.ok(roomService.getAllRooms(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MeetingRoom> updateRoom(@PathVariable Long id, @RequestBody MeetingRoom roomDetails) {
        return ResponseEntity.ok(roomService.updateRoom(id, roomDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}