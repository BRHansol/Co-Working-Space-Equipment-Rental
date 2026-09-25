package com.example.roombooking.service.validation;

import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.dto.request.BookingCreateRequest;
import com.example.roombooking.exception.ForbiddenException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
public class UserPermissionHandler extends BookingValidationHandler{
    @Override
    protected void doValidate(BookingValidationContext context) {
        User requester = context.getRequester();
        BookingCreateRequest request = context.getRequest();
 
        log.debug("[Validation] ตรวจสอบสิทธิ์ผู้จอง userId={}", requester.getId());
 
        if (Boolean.FALSE.equals(requester.getActive())) {
            throw new ForbiddenException("บัญชีผู้ใช้นี้ถูกระงับการใช้งาน ไม่สามารถจองห้องประชุมได้");
        }
 
        boolean bookingForSomeoneElse = request.getBookingForUserId() != null
                && !request.getBookingForUserId().equals(requester.getId());
 
        if (bookingForSomeoneElse
                && requester.getRole() != Role.ADMIN
                && requester.getRole() != Role.STAFF) {
            throw new ForbiddenException("ไม่มีสิทธิ์จองห้องประชุมแทนผู้ใช้อื่น");
        }
    }
}
