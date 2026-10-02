package com.example.roombooking.service.strategy;

import com.example.roombooking.domain.enums.RoomType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BookingRuleStrategyFactory {

    private final Map<RoomType, BookingRuleStrategy> strategies = new HashMap<>();

    // Spring ส่ง strategy ทุกตัวที่เป็น @Component เข้ามาให้เอง
    public BookingRuleStrategyFactory(List<BookingRuleStrategy> strategyList) {
        for (BookingRuleStrategy strategy : strategyList) {
            strategies.put(strategy.getRoomType(), strategy);
        }
    }

    public BookingRuleStrategy getStrategy(RoomType roomType) {
        BookingRuleStrategy strategy = strategies.get(roomType);
        if (strategy == null) {
            throw new IllegalArgumentException("ไม่มี strategy สำหรับห้องประเภท " + roomType);
        }
        return strategy;
    }
}