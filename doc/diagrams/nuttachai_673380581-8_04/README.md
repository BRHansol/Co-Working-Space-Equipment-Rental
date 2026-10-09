# 9.1 Diagrams — งานคนที่ 4

**นายณัฏฐชัย ผลดี — nuttachai_673380581-8_04**  
ขอบเขต: Booking Validation, Equipment Linking, Booking DTO และ Mapper

เอกสารชุดนี้อ้างอิงข้อ 9.1 ในใบงาน CP353002 และโค้ดใน branch `nuttachai_673380581-8_04` ที่ตรวจเมื่อ 9 ตุลาคม 2026 ภาพเน้นส่วนที่คนที่ 4 รับผิดชอบ ส่วน Booking Core, Strategy, State, Authentication และ Deployment แสดงเป็นบริบทของระบบที่เชื่อมต่อกันตามโค้ด ไม่ได้เปลี่ยนผู้รับผิดชอบงานเหล่านั้น

## แผนผังเอกสารตามใบงาน

| Diagram ที่ใบงานกำหนด | เอกสาร | ขอบเขตของคนที่ 4 |
| --- | --- | --- |
| Use Case Diagram + Use Case Description | [01-use-cases.md](01-use-cases.md) | ตรวจเงื่อนไขและผูกอุปกรณ์เมื่อสร้าง/แก้ไขการจอง |
| Domain Model / Conceptual Class Diagram | [02-domain-and-class.md](02-domain-and-class.md) | Booking ↔ BookingEquipment ↔ Equipment และข้อมูลประกอบการตรวจ |
| Class Diagram พร้อมตำแหน่ง Design Pattern | [02-domain-and-class.md](02-domain-and-class.md) | Chain of Responsibility, Context, DTO, Mapper และ Repository |
| Sequence Diagram อย่างน้อย 3 Scenario | [03-sequence-diagrams.md](03-sequence-diagrams.md) | สร้างสำเร็จ, แก้ไขสำเร็จ, อุปกรณ์ไม่พอ |
| Activity Diagram | [04-validation-activity.md](04-validation-activity.md) | ลำดับตรวจเงื่อนไขและหยุดเมื่อพบข้อผิดพลาด |
| ER Diagram / Database Schema | [05-er-diagram.md](05-er-diagram.md) | ตาราง `booking_equipment`, FK, quantity และ index |
| Component Diagram & Deployment Diagram | [06-component-and-deployment.md](06-component-and-deployment.md) | ส่วนของคนที่ 4 ภายใน Spring Boot และบริบท Railway/Aiven |
| State Diagram | [07-booking-state.md](07-booking-state.md) | บริบทสถานะ Booking ของคนที่ 3 ที่เกี่ยวข้องกับ validation |

ไฟล์ Markdown มีภาพ Mermaid ซึ่งเปิดดูได้บน GitHub หรือโปรแกรมที่รองรับ Mermaid ส่วน Use Case มี [ต้นฉบับ PlantUML](01-use-cases.puml) เพิ่มเติมสำหรับสัญลักษณ์ UML ของ Actor, Use Case, `include` และ `extend`

## จุดสำคัญที่ภาพยึดจากโค้ด

- Chain ใช้ `List<BookingValidationHandler>` ที่ Spring เรียงตาม `@Order` เป็น **สิทธิ์ผู้ใช้ → สถานะห้อง → เวลาซ้อนทับ → จำนวนอุปกรณ์** การโยน exception หยุดการตรวจทันที
- Handler เก็บห้องและจำนวนอุปกรณ์ที่รวมแล้วใน `BookingValidationContext` เมื่อผ่าน Service จึงใช้ Mapper และสร้าง `BookingEquipment`
- `BookingEquipment` ทำให้ Booking กับ Equipment มีความสัมพันธ์ Many-to-Many ในเชิงโดเมน แต่โค้ดใช้ join entity กับ `@ManyToOne` สองด้าน เพื่อเก็บ `quantity`
- `sumReservedQuantity()` คืนยอดจอง **พร้อมกันสูงสุด** ของ `PENDING` / `APPROVED` ในช่วง `[startTime, endTime)` โดยตัด Booking เดิมออกตอนแก้ไข
- คำขอไม่ผ่าน validation ไม่มี Booking ใหม่ถูกบันทึก จึงไม่ใช่การเปลี่ยนสถานะเป็น `REJECTED`

## การแบ่งความรับผิดชอบในภาพ

| สมาชิก | ส่วนที่นำมาแสดงเป็นบริบท |
| --- | --- |
| คนที่ 1 — นายยุทธนา เหล่าวิสัย | User, Authentication และ Security |
| คนที่ 2 — นายกฤติธี ศรีใสย์ | MeetingRoom, Equipment และ Strategy |
| คนที่ 3 — นายบุญปวีณ เรืองไพศาล | BookingController, BookingService, Booking และ State |
| คนที่ 4 — นายณัฏฐชัย ผลดี | BookingEquipment, validation chain/handlers/context, Booking DTO/Mapper และ Repository ของ join entity |
| คนที่ 5 — นายจีรภัทร แก้วดี | ErrorResponse, GlobalExceptionHandler, Observer และ Infra/Deployment |

ชุดนี้ครอบคลุมชนิด Diagram ตามข้อ 9.1 **เฉพาะขอบเขตงานคนที่ 4 และบริบทที่จำเป็น** เอกสารภาพรวมโครงการและรายละเอียดโมดูลของสมาชิกอื่นยังต้องรวมในงานส่งของทีม
