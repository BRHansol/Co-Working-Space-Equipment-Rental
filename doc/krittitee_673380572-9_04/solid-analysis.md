# SOLID Analysis: ส่วน Room / Equipment / Booking Rule Strategy

ผู้รับผิดชอบ: กฤติธี ศรีใสย์ (673380572-9), branch `krittitee_673380572-9_04`
Path ในตารางย่อจาก `code/src/main/java/com/example/roombooking/`

## S: Single Responsibility

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `controller/api/RoomController.java` | 15–49 | รับ HTTP request และคืน status code อย่างเดียว ไม่มี business logic และไม่แตะ repository |
| `dto/request/RoomCreateRequest.java` | 11–25 | ตรวจรูปแบบข้อมูลขาเข้าด้วย Bean Validation แยกจาก service |
| `service/impl/RoomServiceImpl.java` | 18–83 | ทำ business logic ของห้อง (หาไม่เจอ → 404, ลบติดการจอง → 409) อย่างเดียว |
| `mapper/RoomMapper.java` | 9–33 | แปลง Entity ↔ DTO อย่างเดียว |
| `exception/GlobalExceptionHandler.java` | 63–64 | แปลง `ConflictException` เป็น HTTP 409 ที่เดียว ไม่ต้องจัดการ error ซ้ำในทุก controller |

Equipment แยกหน้าที่แบบเดียวกันใน `EquipmentController`, `EquipmentCreateRequest`, `EquipmentServiceImpl`, `EquipmentMapper`

## O: Open/Closed

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `service/strategy/BookingRuleStrategy.java` | 5–11 | interface กลางของกฎการจอง |
| `service/strategy/StandardRoomRuleStrategy.java` | 7, 16 | กฎห้อง STANDARD: ไม่ต้องอนุมัติ |
| `service/strategy/VipRoomRuleStrategy.java` | 7, 16 | กฎห้อง VIP: ต้องอนุมัติ |
| `service/strategy/BookingRuleStrategyFactory.java` | 16–20 | รับ strategy ทุกตัวจาก Spring อัตโนมัติ |
| `service/impl/BookingServiceImpl.java` | 74–76 | เรียกผ่าน `requiresApproval()` ไม่มี `if (roomType == VIP)` |

ถ้าเพิ่มห้องประเภทใหม่ แค่ **เพิ่มคลาส** strategy ใหม่ ไม่ต้อง **แก้** `BookingServiceImpl` หรือ Factory

## L: Liskov Substitution

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `service/strategy/StandardRoomRuleStrategy.java` | 7–18 | ใช้แทน `BookingRuleStrategy` ได้ทุกที่ คืนค่าครบทุก method ไม่ throw `UnsupportedOperationException` |
| `service/strategy/VipRoomRuleStrategy.java` | 7–18 | เช่นเดียวกัน `BookingServiceImpl` ไม่ต้องรู้ว่าได้ตัวไหน (บรรทัด 74) |
| `service/impl/RoomServiceImpl.java` | 18 | implement `RoomService` ครบทุก method ใช้แทน interface ได้ |

## I: Interface Segregation

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `service/RoomService.java` | 8 | interface ของห้องแยกจากของอุปกรณ์ |
| `service/EquipmentService.java` | 8 | controller อุปกรณ์รู้จักเฉพาะ method ของอุปกรณ์ |
| `service/strategy/BookingRuleStrategy.java` | 8, 11 | มีแค่ 2 method ที่จำเป็น (`getRoomType`, `requiresApproval`) ไม่ใช่ fat interface |

## D: Dependency Inversion

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `controller/api/RoomController.java` | 17–21 | ขึ้นกับ interface `RoomService` ไม่ใช่ `RoomServiceImpl` + constructor injection |
| `controller/api/EquipmentController.java` | 17–21 | ขึ้นกับ interface `EquipmentService` + constructor injection |
| `service/impl/RoomServiceImpl.java` | 20–26 | รับ `MeetingRoomRepository` (interface) ผ่าน constructor, field เป็น `final` |
| `service/strategy/BookingRuleStrategyFactory.java` | 13, 16 | ถือ `BookingRuleStrategy` (interface) ไม่ผูกกับคลาสจริง |
| `service/impl/BookingServiceImpl.java` | 38–55 | รับ Factory ผ่าน constructor ไม่ได้ `new` เอง |
