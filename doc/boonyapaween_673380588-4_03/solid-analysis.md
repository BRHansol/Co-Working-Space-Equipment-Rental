# SOLID Analysis: ส่วน Booking Core และ State Pattern

ผู้รับผิดชอบ: บุญปวีณ เรืองไพศาล (673380588-4), branch `boonyapaween_673380588-4_03`

Path ในตารางย่อจาก `code/src/main/java/com/example/roombooking/` เลขบรรทัดอ้างอิง branch `develop`

## S: Single Responsibility

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `domain/state/PendingState.java` | 5–31 | รู้แค่กฎของสถานะ PENDING (อนุมัติ, ปฏิเสธ, ยกเลิก, แก้ไขได้) ไม่รู้จัก DB หรือ HTTP |
| `domain/state/ApprovedState.java` | 5–21 | รู้แค่กฎของสถานะ APPROVED (ยกเลิก, ใช้ห้องเสร็จ) |
| `domain/state/BookingContext.java` | 6–55 | ถือ state ปัจจุบันและ sync สถานะกลับไปที่ entity อย่างเดียว ไม่มีกฎของสถานะใดอยู่ในคลาสนี้ |
| `controller/api/BookingController.java` | 15–69 | รับ HTTP request, อ่าน header และ path, คืน status code แล้วส่งต่อ service ทันที ไม่มี business logic |
| `repository/BookingRepository.java` | 13–34 | เข้าถึงข้อมูลการจองอย่างเดียว |
| `service/impl/BookingServiceImpl.java` | 150–154 | การตรวจเงื่อนไขการจองมอบให้ `BookingValidationChain` ไม่ตรวจเองใน service |
| `exception/InvalidStateTransitionException.java` | 5–10 | แทนความผิดพลาดเดียวคือเปลี่ยนสถานะผิดกฎ แปลงเป็น 409 ที่ `GlobalExceptionHandler` |

## O: Open/Closed

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `domain/state/BookingState.java` | 22–36 | default method ปฏิเสธทุก action ไว้ก่อน state ใหม่จึง override แค่ทางที่ไปได้ เปลี่ยนกฎของสถานะใดก็แก้แค่คลาสนั้น |
| `service/impl/BookingServiceImpl.java` | 109–111, 138 | service ไม่มี `if (status == ...)` เลย ส่งให้ `BookingContext` ตัดสิน กฎของสถานะเปลี่ยนได้โดยไม่แตะ service |
| `service/impl/BookingServiceImpl.java` | 150–154 | เพิ่มกฎการตรวจใหม่ทำได้โดยเพิ่ม handler ใน chain ไม่ต้องแก้ service |
| `service/impl/BookingServiceImpl.java` | 141–142 | เพิ่มช่องทางแจ้งเตือนใหม่ได้โดยเพิ่ม listener ของ event ไม่ต้องแก้ service |

`resolveState()` (`BookingContext.java` 46–54) และ `switch` ใน `updateStatus()` (`BookingServiceImpl.java` 126–132) เป็นการแปลงค่า enum เป็น object หรือ action ที่จุดเดียว ไม่ได้กระจายกฎ การเพิ่มสถานะต้องเพิ่มค่าใน enum อยู่แล้ว และ Java บังคับให้ `switch` บน enum ครบทุกค่า ลืมเพิ่มจะ compile ไม่ผ่าน

## L: Liskov Substitution

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `domain/state/BookingState.java` | 6–12 | contract เขียนไว้ชัด: transition ที่อนุญาตเปลี่ยนสถานะ ที่ไม่อนุญาต throw `InvalidStateTransitionException` |
| `domain/state/BookingState.java` | 22–40 | ทุก state ใช้ default method ชุดเดียวกันในการปฏิเสธ จึงทำตาม contract เหมือนกันทุกตัว ไม่มีตัวไหน throw `UnsupportedOperationException` |
| `domain/state/CompletedState.java` | 5–11 | override แค่ `getStatus()` แต่ยังใช้แทน `BookingState` ได้ครบทุก method โดยไม่พังตรรกะ |
| `domain/state/BookingContext.java` | 16–34 | เรียกทุก action ผ่าน interface โดยไม่รู้ว่า state จริงเป็นคลาสไหน |
| `service/impl/BookingServiceImpl.java` | 30 | implement `BookingService` ครบทุก method ใช้แทน interface ได้ทุกที่ |

หลักฐานจาก test: `BookingStateTest` ทดสอบครบ 20 คู่ (5 สถานะ × 4 action) ยืนยันว่าทุก state ทำตาม contract เดียวกัน และสถานะไม่เปลี่ยนเมื่อถูกปฏิเสธ

## I: Interface Segregation

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `domain/state/BookingState.java` | 13–41 | มีเฉพาะ action ของการจอง 4 ตัว + `isEditable()` + `getStatus()` ทุก method เกี่ยวกับทุก state ไม่ใช่ fat interface |
| `service/BookingService.java` | 9–22 | มีเฉพาะงานของการจอง แยกจาก `RoomService`, `EquipmentService`, `UserService` controller การจองจึงไม่ต้องรู้จัก method ของห้องหรือผู้ใช้ |
| `repository/BookingRepository.java` | 13–34 | query เฉพาะของ Booking ไม่ปนกับ repository อื่น |

## D: Dependency Inversion

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `controller/api/BookingController.java` | 19–23 | ขึ้นกับ interface `BookingService` ไม่ใช่ `BookingServiceImpl` + constructor injection, field เป็น `final` |
| `service/impl/BookingServiceImpl.java` | 32–54 | dependency ทั้ง 7 ตัวเข้าทาง constructor ไม่มี `@Autowired` บน field และไม่ `new` เอง repository ทุกตัวเป็น interface |
| `service/impl/BookingServiceImpl.java` | 141–142 | ส่ง event ผ่าน `ApplicationEventPublisher` ไม่รู้จัก `NotificationListener` เลย |
| `domain/state/BookingContext.java` | 9 | ถือ `BookingState` (interface) ไม่ผูกกับคลาส state ตัวใด |

ประโยชน์ที่ใช้จริง: `BookingControllerTest` ใส่ mock ของ `BookingService` แทนได้ทันที และ `BookingServiceImplTest` ใส่ mock ของ repository และ chain ได้โดยไม่ต้องแก้โค้ด
