# Design Patterns: ส่วน Booking Core และ State Pattern

ผู้รับผิดชอบ: บุญปวีณ เรืองไพศาล (673380588-4), branch `boonyapaween_673380588-4_03`

กลุ่ม GoF: **Behavioral**

| Pattern | ปัญหาที่แก้ | ไฟล์/คลาสที่ใช้ |
|---|---|---|
| **State** (Behavioral) | การจองแต่ละสถานะทำได้ไม่เหมือนกัน (PENDING อนุมัติได้, COMPLETED เปลี่ยนต่อไม่ได้) ถ้าเขียน `if (status == ...)` ทุก method ต้องเช็คสถานะซ้ำ และต้องแก้หลายที่เมื่อกฎเปลี่ยน | `BookingState` (interface), `PendingState`, `ApprovedState`, `RejectedState`, `CancelledState`, `CompletedState`, `BookingContext`, ใช้งานใน `BookingServiceImpl` บรรทัด 74–76, 109–111, 126–138 |
| Repository (Enterprise) | แยกการเข้าถึงข้อมูลออกจาก business logic | `BookingRepository` (Spring Data JPA, derived query ไม่มี SQL) |
| Service Layer (Enterprise) | รวม business logic ของการจองไว้ที่เดียว และเป็นจุดที่ Pattern ของทีมมาประกอบกัน | `BookingService`, `BookingServiceImpl` |
| MVC (Enterprise) | แยกการรับ HTTP ออกจาก logic | `BookingController` (Controller), `BookingResponse` (Model ที่ส่งออก) |
| DTO + Mapper (Enterprise) | ไม่ส่ง `Booking` entity ออก API ตรงๆ | รับ `BookingCreateRequest`, คืน `BookingResponse` ผ่าน `BookingMapper` |
| Dependency Injection (Enterprise) | ไม่ผูกกับคลาสจริง ทดสอบด้วย mock ได้ | constructor injection ใน `BookingController` และ `BookingServiceImpl` |

## State

### ทำไมเลือก State

- การจองมี "สถานะ" ที่เปลี่ยนพฤติกรรมของ object ชัดเจน ซึ่งเป็นกรณีคลาสสิกของ State Pattern
- กฎของแต่ละสถานะอยู่ในคลาสเดียว อ่านง่าย แก้ที่เดียว (Single Responsibility, Open/Closed)
- ทุก state ทำตาม contract เดียวกัน ใช้แทนกันได้ (Liskov Substitution)
- ทดสอบได้ครบทุกกรณีด้วยตารางเดียว (`BookingStateTest` 20 คู่)

### ออกแบบอย่างไร

1. **`BookingState` ใช้ default method** ทุก action มีค่าเริ่มต้นเป็น throw `InvalidStateTransitionException` concrete state จึง override เฉพาะทางที่ไปได้ สถานะสุดท้าย (`CompletedState`, `CancelledState`, `RejectedState`) จึงเหลือแค่ `getStatus()` ไม่ต้องมี abstract class แยก
2. **`BookingContext` เชื่อมกับฐานข้อมูล** ค่าจริงอยู่ที่ enum `status` ของ entity context สร้าง state object จาก enum ตอนโหลด (`resolveState`) และ sync กลับเมื่อเปลี่ยน (`setState`)
3. **`setState()` เป็น package-private** มีแค่คลาสใน `domain.state` ที่เปลี่ยน state ได้ ภายนอกต้องเรียกผ่าน `approve()`, `cancel()` ฯลฯ ซึ่งผ่านกฎเสมอ
4. **`isEditable()`** ให้ State ตัดสินว่าแก้ไขรายละเอียดได้ไหม (`updateBooking()` บรรทัด 109–111) แทนการเขียน `if (status == PENDING)` ใน service
5. **exception เฉพาะ** `InvalidStateTransitionException` แยกจาก `IllegalStateException` ของ Java จึง map เป็น 409 ได้แม่นและ test เช็คชนิดได้ตรง

### ใช้งานที่ไหน

| จุดเรียก | บรรทัดใน `BookingServiceImpl` | ทำอะไร |
|---|---|---|
| `createBooking()` | 74–76 | ห้องที่ Strategy บอกว่าไม่ต้องรออนุมัติ เรียก `approve()` ก่อน save |
| `updateBooking()` | 109–111 | เช็ค `isEditable()` แก้ได้เฉพาะ PENDING |
| `updateStatus()` | 126–138 | แปลงสถานะปลายทางเป็น action แล้วให้ state ตัดสิน |

### ทำงานร่วมกับ Pattern อื่นยังไง

- **Strategy (คนที่ 2)**: Strategy ตัดสินว่า *ควร* อนุมัติอัตโนมัติไหม ส่วน State ตัดสินว่า *อนุมัติได้* ไหม Strategy จึงไม่ set status เอง แต่เรียก `BookingContext.approve()`
- **Observer (คนที่ 5)**: หลังเปลี่ยนสถานะสำเร็จและ save แล้ว `updateStatus()` ส่ง `BookingStatusChangedEvent(oldStatus, newStatus, changedBy)` ให้ listener บันทึกประวัติ ถ้า state ปฏิเสธจะไม่มี event
- **Chain of Responsibility (คนที่ 4)**: ตอนแก้ไข State เช็ค `isEditable()` **ก่อน** เข้า chain การจองที่แก้ไม่ได้จึงไม่เสียเวลาตรวจเงื่อนไขอื่น

### ทางเลือกที่พิจารณาแล้วไม่เลือก

| ทางเลือก | เหตุผลที่ไม่เลือก |
|---|---|
| `if/else` หรือ `switch` ใน service | กฎกระจายหลาย method เพิ่มสถานะต้องแก้ทุกจุด |
| ใส่ method ใน enum `BookingStatus` | enum ปนทั้งค่าที่เก็บ DB และพฤติกรรม test แยกยาก และขยายไม่สะดวกเท่าคลาส |
| abstract class แทน default method | ต้องมีคลาสเพิ่มโดยไม่จำเป็น และ state จะ extend คลาสอื่นไม่ได้อีก |
| เก็บ state object ลง DB | ต้องแปลงไปมา ใช้ enum เป็นค่าจริงแล้วสร้าง state ตอนใช้ง่ายและปลอดภัยกว่า |

## Diagram

- [diagrams/class-state.md](diagrams/class-state.md) Class Diagram ของ State Pattern
- [diagrams/state-booking.md](diagrams/state-booking.md) State Diagram และตาราง transition ครบ 20 คู่
- [diagrams/sequence-update-status.md](diagrams/sequence-update-status.md) Sequence ของการเปลี่ยนสถานะ
