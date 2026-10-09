# Design Patterns: ส่วน Room / Equipment / Booking Rule Strategy

ผู้รับผิดชอบ: กฤติธี ศรีใสย์ (673380572-9), branch `krittitee_673380572-9_04`
กลุ่ม GoF: **Behavioral**

| Pattern | ปัญหาที่แก้ | ไฟล์/คลาสที่ใช้ |
|---|---|---|
| **Strategy** (Behavioral) | ห้องแต่ละประเภทมีกฎการอนุมัติต่างกัน (STANDARD อนุมัติอัตโนมัติ, VIP ต้องรอ admin) ถ้าเขียน `if (type == VIP) ... else ...` ใน service ทุกครั้งที่เพิ่มประเภทห้องต้องกลับไปแก้ service | `BookingRuleStrategy` (interface), `StandardRoomRuleStrategy`, `VipRoomRuleStrategy`, ใช้งานใน `BookingServiceImpl.createBooking()` บรรทัด 74–76 |
| Factory (ตัวช่วยเลือก Strategy) | ต้องมีที่เดียวสำหรับเลือก strategy ให้ตรงกับ `RoomType` และแจ้ง error ชัดเจนถ้าไม่มีกฎ | `BookingRuleStrategyFactory.getStrategy()` |
| Service Layer + Repository + DTO/Mapper (Enterprise) | แยก Room/Equipment เป็นชั้น controller → service → repository และไม่ส่ง Entity ออก API | `RoomController` → `RoomService` → `MeetingRoomRepository`, `RoomMapper` (Equipment เหมือนกัน) |

## Strategy

### ทำไมเลือก Strategy
- กฎที่เปลี่ยนตาม "ประเภท" ของสิ่งของคือกรณีคลาสสิกของ Strategy
- ทำให้เพิ่มประเภทห้องใหม่ได้โดยเพิ่มคลาส ไม่ต้องแก้โค้ดเดิม (Open/Closed)
- ทดสอบกฎแต่ละตัวแยกกันได้ (`BookingRuleStrategyTest`)

### ทำงานร่วมกับ Pattern อื่นยังไง
- **State**: ถ้าไม่ต้องอนุมัติ Strategy จะเรียก `BookingContext.approve()` เพื่อเปลี่ยน PENDING → APPROVED ผ่าน state machine ไม่ได้ set status ตรง ๆ
- **Chain of Responsibility**: validation chain หาห้องให้ก่อน แล้ว Strategy ใช้ `room.getRoomType()` จากผลนั้น

### Class Diagram

```mermaid
classDiagram
    direction LR
    class BookingRuleStrategy {
        <<interface>>
        +getRoomType() RoomType
        +requiresApproval() boolean
    }
    class StandardRoomRuleStrategy {
        +getRoomType() RoomType  // STANDARD
        +requiresApproval() boolean  // false
    }
    class VipRoomRuleStrategy {
        +getRoomType() RoomType  // VIP
        +requiresApproval() boolean  // true
    }
    class BookingRuleStrategyFactory {
        -Map~RoomType, BookingRuleStrategy~ strategies
        +BookingRuleStrategyFactory(List~BookingRuleStrategy~)
        +getStrategy(RoomType) BookingRuleStrategy
    }
    class BookingServiceImpl {
        -BookingRuleStrategyFactory ruleStrategyFactory
        +createBooking(BookingCreateRequest, Long) BookingResponse
    }
    class BookingContext {
        +approve()
    }
    class RoomType {
        <<enumeration>>
        STANDARD
        VIP
    }

    BookingRuleStrategy <|.. StandardRoomRuleStrategy
    BookingRuleStrategy <|.. VipRoomRuleStrategy
    BookingRuleStrategyFactory o--> "*" BookingRuleStrategy : strategies
    BookingServiceImpl --> BookingRuleStrategyFactory : getStrategy(roomType)
    BookingServiceImpl ..> BookingContext : approve() ถ้าไม่ต้องอนุมัติ
    BookingRuleStrategy ..> RoomType
```
