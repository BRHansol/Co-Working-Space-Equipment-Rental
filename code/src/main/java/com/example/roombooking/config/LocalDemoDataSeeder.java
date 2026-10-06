package com.example.roombooking.config;

import com.example.roombooking.controller.web.support.WebSessionSupport;
import com.example.roombooking.domain.entity.Equipment;
import com.example.roombooking.domain.entity.MeetingRoom;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.domain.enums.RoomStatus;
import com.example.roombooking.domain.enums.RoomType;
import com.example.roombooking.repository.EquipmentRepository;
import com.example.roombooking.repository.MeetingRoomRepository;
import com.example.roombooking.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@Profile("local")
public class LocalDemoDataSeeder implements ApplicationRunner {
    private final UserRepository users;
    private final MeetingRoomRepository rooms;
    private final EquipmentRepository equipment;
    private final WebSessionSupport sessions;

    public LocalDemoDataSeeder(UserRepository users, MeetingRoomRepository rooms,
                               EquipmentRepository equipment, WebSessionSupport sessions) {
        this.users = users;
        this.rooms = rooms;
        this.equipment = equipment;
        this.sessions = sessions;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() != 0 || rooms.count() != 0 || equipment.count() != 0) {
            return;
        }
        createUser("narin", Role.USER);
        createUser("staff", Role.STAFF);
        createUser("admin", Role.ADMIN);
        createRoom("ห้อง Focus", 8, "2", RoomType.STANDARD, RoomStatus.AVAILABLE);
        createRoom("ห้อง Studio", 12, "2", RoomType.VIP, RoomStatus.AVAILABLE);
        createRoom("ห้อง Board", 10, "3", RoomType.VIP, RoomStatus.AVAILABLE);
        createRoom("ห้อง Quiet", 1, "2", RoomType.STANDARD, RoomStatus.AVAILABLE);
        createRoom("ห้อง Flow", 6, "1", RoomType.STANDARD, RoomStatus.AVAILABLE);
        createRoom("ห้อง Workshop", 16, "3", RoomType.STANDARD, RoomStatus.MAINTENANCE);
        createEquipment("โปรเจกเตอร์", 5, "ภาพและเสียง");
        createEquipment("ไมโครโฟน", 8, "เสียง");
        createEquipment("จอภาพ", 6, "จอแสดงผล");
        createEquipment("สาย HDMI", 12, "สายเชื่อมต่อ");
        createEquipment("ลำโพง", 4, "เสียง");
        createEquipment("ไวท์บอร์ด", 3, "อุปกรณ์ประชุม");
    }

    private void createUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword(sessions.hashPassword("local123"));
        user.setRole(role);
        user.setActive(true);
        user.setCreated_at(LocalDate.now(ZoneId.of("Asia/Bangkok")));
        users.save(user);
    }

    private void createRoom(String name, int capacity, String floor, RoomType type, RoomStatus status) {
        MeetingRoom room = new MeetingRoom();
        room.setName(name);
        room.setCapacity(capacity);
        room.setFloor(floor);
        room.setRoomType(type);
        room.setStatus(status);
        rooms.save(room);
    }

    private void createEquipment(String name, int quantity, String category) {
        Equipment item = new Equipment();
        item.setName(name);
        item.setTotalQuantity(quantity);
        item.setCategory(category);
        equipment.save(item);
    }
}
