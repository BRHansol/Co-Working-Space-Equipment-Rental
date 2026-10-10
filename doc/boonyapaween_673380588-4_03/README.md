# ส่วนงานคนที่ 3: Booking Core และ State Pattern

| | |
|---|---|
| ชื่อ | นายบุญปวีณ เรืองไพศาล |
| รหัสนักศึกษา | 673380588-4 |
| Branch | `boonyapaween_673380588-4_03` |
| รับผิดชอบ | Booking entity และ BookingStatus, BookingController / BookingService / BookingRepository, CRUD การจอง, State Pattern ของสถานะการจอง, pagination และ sorting |

## เอกสารในโฟลเดอร์นี้

| ไฟล์ | เนื้อหา |
|---|---|
| [solid-analysis.md](solid-analysis.md) | SOLID ที่ปรากฏในส่วนงานนี้ พร้อมไฟล์และบรรทัด |
| [design-patterns.md](design-patterns.md) | State Pattern และ Enterprise Pattern ในส่วน Booking Core พร้อม Class Diagram |
| [api-booking.md](api-booking.md) | Endpoint ของ Booking, request/response, status code, pagination |
| [diagrams/class-booking-core.md](diagrams/class-booking-core.md) | Class Diagram: Booking Core แยกตาม Layer |
| [diagrams/class-state.md](diagrams/class-state.md) | Class Diagram: State Pattern |
| [diagrams/state-booking.md](diagrams/state-booking.md) | State Diagram: วงจรสถานะการจอง + ตาราง transition 20 คู่ |
| [diagrams/sequence-create-booking.md](diagrams/sequence-create-booking.md) | Sequence: สร้างการจอง (Chain, Strategy, State) |
| [diagrams/sequence-update-booking.md](diagrams/sequence-update-booking.md) | Sequence: แก้ไขการจอง (เฉพาะ PENDING) |
| [diagrams/sequence-update-status.md](diagrams/sequence-update-status.md) | Sequence: เปลี่ยนสถานะสำเร็จ และเปลี่ยนผิดกฎได้ 409 |
| `diagrams/*.puml` | ซอร์ส PlantUML ของทั้ง 6 diagram |
| `diagrams/png/` | รูป PNG ของทั้ง 6 diagram ไว้ใส่สไลด์ |

## โค้ดที่รับผิดชอบ

```
code/src/main/java/com/example/roombooking/
├── domain/entity/Booking.java
├── domain/enums/BookingStatus.java
├── domain/state/
│   ├── BookingState.java          (interface)
│   ├── BookingContext.java
│   ├── PendingState.java
│   ├── ApprovedState.java
│   ├── RejectedState.java
│   ├── CancelledState.java
│   └── CompletedState.java
├── exception/InvalidStateTransitionException.java
├── repository/BookingRepository.java
├── service/BookingService.java
├── service/impl/BookingServiceImpl.java
├── controller/api/BookingController.java
└── common/PageResponse.java
```

## การทดสอบ (Tests)

ผลจาก [Test Report ของทีม](../test-report/README.md): **4 classes, 61 tests ผ่านทั้งหมด**

| Test class | ประเภท | Tests | ทดสอบอะไร |
|---|---|---:|---|
| `BookingStateTest` | JUnit 5 (`@ParameterizedTest`) | 27 | ครบ 20 คู่ (5 สถานะ × 4 action), แก้ไขได้เฉพาะ PENDING 5 กรณี, context ทำงานต่อเนื่อง 2 กรณี |
| `BookingServiceImplTest` | Mockito | 19 | create, read, update, updateStatus ทั้งทางสำเร็จและทาง error, event ส่งพร้อมค่าถูกต้อง |
| `BookingControllerTest` | `@WebMvcTest`, MockMvc | 8 | URL, status code 201/200/409, header `X-User-Id`, pagination และ sorting |
| `BookingRepositoryTest` | `@DataJpaTest` + H2 | 7 | query เวลาซ้อนบนฐานข้อมูลจริง (ชน, ต่อกันพอดี, สถานะที่ยกเลิก, ห้องอื่น), แบ่งหน้าและเรียงลำดับ |
| **รวม** | | **61** | |

```
test/boonyapaween_673380588-4_03/java/com/example/roombooking/
├── domain/state/BookingStateTest.java
├── service/impl/BookingServiceImplTest.java
├── controller/api/BookingControllerTest.java
└── repository/BookingRepositoryTest.java
```

### วิธีรันการทดสอบ

รันเฉพาะ test ของส่วนนี้จากโฟลเดอร์ `code/` (ไม่ต้องต่อฐานข้อมูลจริง ใช้ mock และ H2)

```powershell
.\mvnw.cmd test -P boonyapaween_673380588-4_03
```

## คำถามและแนวทางการตอบตอนนำเสนอ

| คำถาม | แนวทางตอบ |
|---|---|
| ทำไมใช้ State Pattern ไม่ใช้ if else | กฎของแต่ละสถานะต่างกัน if else ต้องเช็คสถานะซ้ำทุก method และแก้หลายที่เมื่อเพิ่มสถานะ State ทำให้กฎอยู่ในคลาสของสถานะนั้นที่เดียว |
| State กับ enum `BookingStatus` ต่างกันยังไง | enum คือค่าที่เก็บใน DB ส่วน state object คือพฤติกรรม `BookingContext` สร้าง state จาก enum ตอนโหลด และ sync กลับเมื่อเปลี่ยน |
| state ที่ throw ทุก method ผิดข้อ L ไหม | ไม่ผิด เพราะการ throw เป็น contract ที่เขียนไว้ใน interface และทุก state ใช้ default method ตัวเดียวกัน ไม่มีตัวไหน throw `UnsupportedOperationException` และ `BookingStateTest` ยืนยันครบ 20 คู่ |
| ทำไม `setState()` ไม่เป็น public | ให้เปลี่ยนสถานะได้เฉพาะคลาส state ภายนอกต้องเรียกผ่าน action ซึ่งผ่านกฎเสมอ |
| ทำไมแก้ไขได้เฉพาะ PENDING | การจองที่อนุมัติแล้วถ้าแก้เวลาหรือห้องได้ จะเท่ากับข้ามการอนุมัติ |
| ถ้าจะเพิ่มสถานะใหม่ต้องแก้อะไร | เพิ่มค่าใน enum, สร้างคลาส state ใหม่, เพิ่มทางเข้าจาก state เดิม และเพิ่มใน `resolveState()` (compiler เตือนถ้าลืม) และแก้ CHECK constraint ใน `schema.sql` service ไม่ต้องแก้ |
| ป้องกันการจองซ้อนยังไง | `findOverlappingBookings` หาการจองที่ยัง active ในห้องเดียวกันที่ `start < end ที่ขอ` และ `end > start ที่ขอ` เจอได้ 409 ตอนแก้ไขส่ง bookingId เพื่อไม่นับตัวเอง |
| ทำไมไม่เขียน SQL หรือ JPQL | derived query ของ Spring Data สร้าง query จากชื่อ method DB ยังกรองและใช้ index เหมือนเดิม แต่เป็น Java ล้วน |
| ทำไม 409 ไม่ใช่ 400 | 400 คือข้อมูลผิดรูปแบบ 409 คือข้อมูลถูกรูปแบบแต่ขัดกับสถานะปัจจุบันของ resource |
| สองคนจองพร้อมกันพอดีล่ะ | ตรวจก่อนบันทึกจึงมีช่องว่างเล็กน้อย วิธีแก้คือ `@Lock(PESSIMISTIC_WRITE)` หรือ exclusion constraint ของ PostgreSQL |
