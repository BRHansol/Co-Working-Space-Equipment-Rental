package com.example.roombooking.service.validation;


import com.example.roombooking.domain.entity.Equipment;
import com.example.roombooking.dto.request.BookingCreateRequest;
import com.example.roombooking.dto.request.BookingCreateRequest.EquipmentItemRequest;
import com.example.roombooking.exception.EquipmentNotAvailableException;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.repository.BookingEquipmentRepository;
import com.example.roombooking.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(4)
public class EquipmentAvailabilityHandler extends BookingValidationHandler {
    private final EquipmentRepository equipmentRepository;
    private final BookingEquipmentRepository bookingEquipmentRepository;

    @Override
    protected void doValidate(BookingValidationContext context) {
        BookingCreateRequest request = context.getRequest();
        List<EquipmentItemRequest> items = request.getEquipmentItems();
        if (items == null || items.isEmpty()) {
            return;
        }

        // รวมรายการอุปกรณ์ชิ้นเดียวกันที่ส่งมาซ้ำ เช่น โปรเจคเตอร์ 2 + 1 = 3
        Map<Long, Integer> requested = context.getRequestedEquipmentQuantities();
        for (EquipmentItemRequest item : items) {
            requested.merge(item.getEquipmentId(), item.getQuantity(), Integer::sum);
        }

        requested.forEach((equipmentId, quantity) -> {
            log.debug("[Validation] ตรวจสอบอุปกรณ์ equipmentId={} จำนวน={}", equipmentId, quantity);

            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("ไม่พบอุปกรณ์ id=" + equipmentId));

            long reserved = bookingEquipmentRepository.sumReservedQuantity(
                    equipmentId, request.getStartTime(), request.getEndTime(), request.getBookingId());
            long available = equipment.getTotalQuantity() - reserved;

            if (quantity > available) {
                throw new EquipmentNotAvailableException(
                        "อุปกรณ์ '" + equipment.getName() + "' ไม่เพียงพอ (ขอ " + quantity + " เหลือ " + available + ")");
            }
        });
    }
}