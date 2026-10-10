package com.example.roombooking.controller.web;

import com.example.roombooking.controller.web.support.WebSessionSupport;
import com.example.roombooking.domain.enums.*;
import com.example.roombooking.dto.response.*;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.repository.BookingRepository;
import com.example.roombooking.repository.EquipmentRepository;
import com.example.roombooking.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

@Controller
@Profile("web")
public class CatalogViewController {
    private final RoomService rooms;
    private final EquipmentService equipment;
    private final BookingService bookings;
    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final WebSessionSupport sessions;
    public CatalogViewController(RoomService rooms, EquipmentService equipment, BookingService bookings,
            BookingRepository bookingRepository, EquipmentRepository equipmentRepository, WebSessionSupport sessions) {
        this.rooms = rooms; this.equipment = equipment; this.bookings = bookings;
        this.bookingRepository = bookingRepository; this.equipmentRepository = equipmentRepository; this.sessions = sessions;
    }
    @GetMapping("/") public String home(Model model) {
        model.addAttribute("rooms", allRooms().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).limit(3).toList());
        model.addAttribute("equipmentList", allEquipment().stream().limit(3).toList());
        model.addAttribute("selectedDate", LocalDate.now(ZoneId.of("Asia/Bangkok")).toString());
        return "pages/home";
    }
    @GetMapping("/account") public String account(HttpServletRequest request) {
        sessions.requireUser(request); return "account/index";
    }
    @GetMapping("/rooms") public String roomList(@RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Integer capacity, @RequestParam(defaultValue = "") String roomType,
            @RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "") String date,
            @RequestParam(defaultValue = "09:00") String startTime, @RequestParam(defaultValue = "11:00") String endTime,
            @RequestParam(defaultValue = "0") int page, Model model) {
        RoomType type = enumFilter(roomType, RoomType.class);
        RoomStatus roomStatus = enumFilter(status, RoomStatus.class);
        LocalDateTime start = null, end = null;
        if (!date.isBlank()) {
            try {
                LocalDate selected = LocalDate.parse(date);
                start = selected.atTime(LocalTime.parse(startTime)); end = selected.atTime(LocalTime.parse(endTime));
                if (!end.isAfter(start)) throw new IllegalArgumentException("เวลาสิ้นสุดต้องอยู่หลังเวลาเริ่มต้น");
            } catch (DateTimeParseException ex) { throw new IllegalArgumentException("กรุณาระบุวันที่และเวลาให้ครบถ้วน"); }
        }
        final LocalDateTime intervalStart = start, intervalEnd = end;
        List<RoomResponse> filtered = allRooms().stream()
                .filter(r -> contains(r.getName(), q) || contains(r.getFloor(), q))
                .filter(r -> capacity == null || r.getCapacity() >= capacity)
                .filter(r -> type == null || r.getRoomType() == type)
                .filter(r -> roomStatus == null || r.getStatus() == roomStatus)
                .filter(r -> intervalStart == null || (r.getStatus() == RoomStatus.AVAILABLE && bookingRepository
                        .findOverlappingBookings(r.getId(), intervalStart, intervalEnd,
                                List.of(BookingStatus.PENDING, BookingStatus.APPROVED)).isEmpty()))
                .toList();
        model.addAttribute("rooms", paginate(filtered, page, 6, model));
        model.addAttribute("q", q); model.addAttribute("capacity", capacity);
        model.addAttribute("roomType", roomType); model.addAttribute("status", status);
        dates(model, date, startTime, endTime);
        return "rooms/list";
    }
    @GetMapping("/rooms/{id}") public String roomDetail(@PathVariable Long id,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(defaultValue = "09:00") String startTime, @RequestParam(defaultValue = "11:00") String endTime,
            Model model) {
        LocalDate selected = date == null ? LocalDate.now(ZoneId.of("Asia/Bangkok")) : date;
        try {
            if (!LocalTime.parse(endTime).isAfter(LocalTime.parse(startTime))) throw new IllegalArgumentException("เวลาสิ้นสุดต้องอยู่หลังเวลาเริ่มต้น");
        } catch (DateTimeParseException ex) { throw new IllegalArgumentException("กรุณาระบุเวลาให้ถูกต้อง"); }
        model.addAttribute("room", rooms.getRoomById(id));
        model.addAttribute("bookings", bookings.getBookingsByRoom(id, Pageable.unpaged()).getContent().stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.APPROVED)
                .filter(b -> b.getStartTime().isBefore(selected.plusDays(1).atStartOfDay()) && b.getEndTime().isAfter(selected.atStartOfDay()))
                .sorted(Comparator.comparing(BookingResponse::getStartTime)).toList());
        dates(model, selected.toString(), startTime, endTime);
        return "rooms/detail";
    }
    @GetMapping("/equipment") public String equipmentList(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String category, @RequestParam(defaultValue = "0") int page, Model model) {
        List<EquipmentResponse> all = allEquipment();
        List<EquipmentResponse> filtered = all.stream().filter(e -> contains(e.getName(), q) || contains(e.getCategory(), q))
                .filter(e -> category.isBlank() || Objects.equals(e.getCategory(), category)).toList();
        model.addAttribute("equipmentList", paginate(filtered, page, 6, model));
        model.addAttribute("categories", all.stream().map(EquipmentResponse::getCategory).filter(Objects::nonNull).distinct().sorted().toList());
        model.addAttribute("q", q); model.addAttribute("category", category); return "equipment/list";
    }
    @GetMapping("/equipment/{id}") public String equipmentDetail(@PathVariable Long id, Model model) {
        if (!equipmentRepository.existsById(id)) throw new ResourceNotFoundException("ไม่พบอุปกรณ์");
        model.addAttribute("equipment", equipment.getEquipmentById(id));
        model.addAttribute("equipmentList", allEquipment().stream().filter(e -> !e.getId().equals(id)).limit(3).toList());
        return "equipment/detail";
    }
    private List<RoomResponse> allRooms() { return rooms.getAllRooms(Pageable.unpaged()).getContent(); }
    private List<EquipmentResponse> allEquipment() { return equipment.getAllEquipments(Pageable.unpaged()).getContent(); }
    private static boolean contains(String text, String q) { return text != null && text.toLowerCase(Locale.ROOT).contains(q.trim().toLowerCase(Locale.ROOT)); }
    private static <E extends Enum<E>> E enumFilter(String text, Class<E> type) {
        if (text.isBlank()) return null;
        try { return Enum.valueOf(type, text); } catch (IllegalArgumentException ex) { throw new IllegalArgumentException("ตัวกรองไม่ถูกต้อง"); }
    }
    private static void dates(Model model, String date, String startTime, String endTime) {
        model.addAttribute("selectedDate", date); model.addAttribute("date", date);
        model.addAttribute("startTime", startTime); model.addAttribute("endTime", endTime);
    }
    public static <T> List<T> paginate(List<T> source, int page, int size, Model model) {
        int pages = Math.max(1, (source.size() + size - 1) / size);
        int safePage = Math.max(0, Math.min(page, pages - 1));
        int start = safePage * size;
        model.addAttribute("pageNumber", safePage); model.addAttribute("totalPages", pages);
        model.addAttribute("totalElements", source.size());
        return source.subList(start, Math.min(start + size, source.size()));
    }
}
