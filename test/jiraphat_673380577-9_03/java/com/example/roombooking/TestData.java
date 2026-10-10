package com.example.roombooking;

import com.example.roombooking.domain.entity.Booking;
import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.BookingStatus;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.domain.enums.RoomStatus;
import com.example.roombooking.domain.enums.RoomType;

import java.time.LocalDateTime;

// สร้าง entity ที่ยังไม่ถูก save สำหรับใช้ใน test ที่ต้องลง DB จริง
public final class TestData {

    private TestData() {
    }

    public static User user(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("secret");
        user.setRole(role);
        return user;
    }

    public static MeetingRoom room(String name) {
        MeetingRoom room = new MeetingRoom();
        room.setName(name);
        room.setCapacity(8);
        room.setFloor("3");
        room.setRoomType(RoomType.STANDARD);
        room.setStatus(RoomStatus.AVAILABLE);
        return room;
    }

    public static Booking pendingBooking(User user, MeetingRoom room) {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        return Booking.builder()
                .user(user)
                .room(room)
                .startTime(start)
                .endTime(start.plusHours(2))
                .status(BookingStatus.PENDING)
                .purpose("Sprint planning")
                .build();
    }
}
