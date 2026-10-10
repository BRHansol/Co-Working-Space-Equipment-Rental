# ส่วนงานคนที่ 2: Room, Equipment และกฎการจอง (Strategy)

| | |
|---|---|
| ชื่อ | กฤติธี ศรีใสย์ |
| รหัสนักศึกษา | 673380572-9 |
| Branch | `krittitee_673380572-9_04` |
| รับผิดชอบ | CRUD ห้องประชุม (Room) และอุปกรณ์ (Equipment), Validation, Error 404/409, Strategy Pattern สำหรับกฎการอนุมัติการจอง |

## เอกสารในโฟลเดอร์นี้

| ไฟล์ | เนื้อหา |
|---|---|
| [solid-analysis.md](solid-analysis.md) | SOLID ที่ปรากฏในส่วนงานนี้ พร้อมไฟล์และบรรทัด |
| [design-patterns.md](design-patterns.md) | Strategy (+ Factory ที่ใช้เลือก strategy) พร้อม Class Diagram |
| [api-room-equipment.md](api-room-equipment.md) | Endpoint ของ Room และ Equipment, request/response, status code |
| [diagrams/class-strategy.md](diagrams/class-strategy.md) | Class Diagram ของ Strategy |
| [diagrams/sequence-create-booking.md](diagrams/sequence-create-booking.md) | Sequence: สร้างการจองแล้วเลือกกฎตามประเภทห้อง |
| [diagrams/sequence-delete-room-409.md](diagrams/sequence-delete-room-409.md) | Sequence: ลบห้องที่มีการจองอยู่ ได้ 409 |
| `diagrams/png/` | รูป PNG ของทั้ง 3 diagram ไว้ใส่สไลด์ |

## โค้ดที่รับผิดชอบ

```
code/src/main/java/com/example/roombooking/
├── controller/api/RoomController.java
├── controller/api/EquipmentController.java
├── dto/request/RoomCreateRequest.java
├── dto/request/EquipmentCreateRequest.java
├── service/RoomService.java, EquipmentService.java
├── service/impl/RoomServiceImpl.java
├── service/impl/EquipmentServiceImpl.java
├── service/impl/BookingServiceImpl.java   (เฉพาะส่วนเรียก Strategy ใน createBooking, ทำร่วมกับคนที่ 3)
└── service/strategy/
    ├── BookingRuleStrategy.java
    ├── StandardRoomRuleStrategy.java
    ├── VipRoomRuleStrategy.java
    └── BookingRuleStrategyFactory.java
```

## Test

เทสต์อยู่ที่ `test/krittitee_673380572-9_04/java/` (รวม 39 test cases)

| ไฟล์ | ทดสอบอะไร |
|---|---|
| `service/impl/RoomServiceImplTest` | CRUD ห้อง, หาไม่เจอ → `ResourceNotFoundException`, ลบห้องที่มีการจอง → `ConflictException` |
| `service/impl/EquipmentServiceImplTest` | CRUD อุปกรณ์, จำนวนติดลบ, 404, ลบอุปกรณ์ที่ถูกจอง → `ConflictException` |
| `service/strategy/BookingRuleStrategyTest` | STANDARD ไม่ต้องอนุมัติ, VIP ต้องอนุมัติ, Factory เลือกถูกตัว |
| `service/impl/BookingServiceImplStrategyTest` | สร้างการจองห้อง STANDARD ได้ APPROVED, ห้อง VIP ได้ PENDING (และสถานะถูก save จริง) |
| `controller/api/RoomControllerTest` | ผ่าน HTTP: 201, 400 (`@Valid`), 404, 204, 409 |
| `controller/api/EquipmentControllerTest` | ผ่าน HTTP: 201, 400 (`@Valid` ทั้ง POST และ PUT), 404, 409 |

### วิธีรัน

```bash
cd code
./mvnw clean test -P krittitee_673380572-9_04
```

Test report อยู่ที่ `code/target/krittitee_673380572-9_04-surefire-reports/`

## ตอบคำถามตอนนำเสนอ

**ทำไมต้อง `flush()` ก่อน catch?**
JPA ไม่ได้ลบทันทีตอนเรียก `delete()` แต่จะรอส่ง SQL ตอน commit ซึ่งเกิดหลังจากออกจาก method ไปแล้ว ถ้าไม่ `flush()` error foreign key จะเกิดนอก `try` แล้วจับไม่ได้ จึงต้อง `flush()` ให้ SQL วิ่งทันทีภายใน `try`

**ทำไมลบแล้วต้องได้ 409 ไม่ใช่ 500?**
500 แปลว่าเซิร์ฟเวอร์พัง แต่กรณีนี้เซิร์ฟเวอร์ไม่ได้พัง ข้อมูลขัดกันเฉย ๆ (ห้องยังมีการจองผูกอยู่) จึงตอบ 409 Conflict ให้ client รู้ว่าต้องยกเลิกการจองก่อน

**Strategy ทำงานยังไง?**
ตอนสร้างการจอง `BookingServiceImpl` ถาม `BookingRuleStrategyFactory` ว่าห้องประเภทนี้ใช้กฎไหน แล้วถาม strategy ว่า `requiresApproval()` ไหม ถ้าไม่ต้อง (STANDARD) ก็ `approve()` ผ่าน State machine เลย ถ้าต้อง (VIP) ก็ค้างที่ PENDING ให้ admin อนุมัติ

**ถ้าเพิ่มห้องประเภทใหม่ เช่น CONFERENCE ต้องแก้อะไร?**
เพิ่ม enum `CONFERENCE` แล้วสร้างคลาส `ConferenceRoomRuleStrategy implements BookingRuleStrategy` ที่มี `@Component` แค่นั้น Spring จะส่งเข้า Factory ให้เอง ไม่ต้องแก้ `BookingServiceImpl` หรือ Factory เลย (Open/Closed)
