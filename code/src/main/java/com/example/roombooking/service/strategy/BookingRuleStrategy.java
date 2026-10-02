package com.example.roombooking.service.strategy;

import com.example.roombooking.domain.enums.RoomType;

public interface BookingRuleStrategy {

    // strategy ตัวนี้ใช้กับห้องประเภทไหน
    RoomType getRoomType();

    // ห้องนี้ต้องรออนุมัติไหม
    boolean requiresApproval();
}