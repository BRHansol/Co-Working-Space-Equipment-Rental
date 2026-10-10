# API: Booking Validation และ Equipment Linking

Base URL: `/api/v1` เอกสารนี้อธิบาย DTO, การตรวจการจอง, การผูกอุปกรณ์ และการแปลง response ใน **ส่วนงานคนที่ 4** ผ่าน endpoint ของ Booking ที่มีอยู่แล้ว ไม่ได้เพิ่ม endpoint ใหม่

`BookingController` / `BookingServiceImpl` เป็นงาน Booking Core ของคนที่ 3 ส่วน Strategy เป็นงานคนที่ 2 และ `GlobalExceptionHandler` / `ErrorResponse` เป็นงานคนที่ 5 การอ้างถึงคลาสเหล่านี้ใช้เพื่ออธิบายจุดเชื่อมต่อกับงานคนที่ 4

Status code ด้านล่างอ้างอิงโค้ด Controller และ Exception Handler ปัจจุบัน **ไม่ได้หมายถึงผลการยิง HTTP หรือผลทดสอบ deployment ในรอบจัดเอกสารนี้**

## Booking: `/api/v1/bookings`

| Method | Path | ทำอะไรในส่วนงานคนที่ 4 | สำเร็จ | Error ที่เกี่ยวข้อง |
|---|---|---|---|---|
| POST | `/api/v1/bookings` | ตรวจ `BookingCreateRequest`, เรียก validation chain, รวมจำนวนอุปกรณ์ซ้ำ, สร้าง `BookingEquipment`, แปลงเป็น `BookingResponse` | 201 | 400, 403, 404, 409 |
| PUT | `/api/v1/bookings/{id}` | ตรวจข้อมูลใหม่โดยไม่นับ Booking เดิม, อัปเดตผ่าน Mapper, แทนรายการอุปกรณ์เดิม | 200 | 400, 403, 404, 409 |

GET และ PATCH เปลี่ยนสถานะอยู่ในงาน Booking Core / State ของคนที่ 3 ไม่ได้เรียก validation chain ทั้งชุดเหมือน POST และ PUT โดย `BookingMapper.toResponse()` ถูกใช้ร่วมกันเพื่อจัดรูปแบบผลลัพธ์

### Profile และผู้ทำรายการ

- เมื่อใช้ profile `api` ซึ่งไม่มี `web` เปิดอยู่ `SecurityConfig` และ `UserIdHeaderInterceptor` ทำงาน: POST / PUT / PATCH ของ Booking ต้องส่ง `X-User-Id` ที่เป็นตัวเลข เช่น `X-User-Id: 11`
- ไม่มี header หรือเป็นค่าว่าง → **401**; header ไม่ใช่ตัวเลข → **400** จาก Interceptor โดยตอบ JSON แบบย่อ ไม่ได้ผ่าน `GlobalExceptionHandler`
- Header นี้ใช้ระบุผู้ทำรายการและให้ Service โหลด User จากฐานข้อมูล ยังไม่ใช่ระบบ authentication ที่ยืนยันตัวตนผู้ส่งด้วย token หรือ session
- เมื่อเปิด profile `web` เช่นกลุ่ม `prod` ที่เป็นค่าเริ่มต้น `WebAccessInterceptor` บล็อก `/api/**` ด้วย **403** หน้า Thymeleaf ใช้ session และ flow ของหน้าเว็บ จึงไม่ควรคาดว่าจะเรียก REST ตัวอย่างด้านล่างผ่านหน้า production แล้วได้ 201 / 200

## Request body

POST และ PUT ใช้ DTO `BookingCreateRequest` เดียวกัน ตัวอย่างสมมติว่ามีผู้ทำรายการ id 11, ห้อง STANDARD id 1 และอุปกรณ์ id 3 ในฐานข้อมูล และคำขอผ่านเงื่อนไขของห้องกับสต็อก

```json
{
  "roomId": 1,
  "startTime": "2026-10-20T09:00:00",
  "endTime": "2026-10-20T11:00:00",
  "purpose": "ประชุมทีมโปรเจกต์",
  "equipmentItems": [
    { "equipmentId": 3, "quantity": 2 },
    { "equipmentId": 3, "quantity": 1 }
  ]
}
```

| Field | กฎ / ความหมาย |
|---|---|
| `bookingId` | ค่าสำหรับตัด Booking เดิมออกจากการตรวจ ไม่ต้องส่งจาก client: Service เขียนทับเป็น `null` ตอน POST และเป็น id ของ Booking ที่โหลดจาก path ตอน PUT |
| `roomId` | ห้าม null; ต้องหาห้องพบและมีสถานะ `AVAILABLE` |
| `bookingForUserId` | ไม่บังคับ; ตอนสร้าง ถ้าไม่ส่งจะจองให้ผู้ทำรายการ ถ้าจองแทนคนอื่นต้องเป็น `ADMIN` หรือ `STAFF` |
| `startTime` | ห้าม null; ใช้ `LocalDateTime` เช่น `2026-10-20T09:00:00` ไม่มี timezone offset ใน DTO |
| `endTime` | ห้าม null และต้องอยู่หลัง `startTime` ตาม `TimeOverlapHandler` |
| `purpose` | ไม่บังคับ; ยาวไม่เกิน 255 ตัวอักษร |
| `equipmentItems` | เป็น `null`, ไม่ส่ง หรือเป็น list ว่างได้ แต่สมาชิกแต่ละรายการห้ามเป็น null |
| `equipmentItems[].equipmentId` | ห้าม null; ต้องหาอุปกรณ์พบ |
| `equipmentItems[].quantity` | ห้าม null และต้องมากกว่า 0; ถ้า id เดียวกันซ้ำจะรวมจำนวนก่อนตรวจสต็อก |

`@Valid` ที่ Controller ตรวจ annotation ของ DTO และรายการอุปกรณ์ซ้อนกันก่อนเรียก Service ส่วนสิทธิ์ผู้ทำรายการ, สถานะห้อง, เวลา และจำนวนคงเหลือตรวจใน chain อีกชั้น การตรวจเวลาปัจจุบันไม่ได้กำหนดว่าต้องเป็นอนาคตด้วย annotation เช่น `@Future`

### ลำดับการตรวจ

| ลำดับ | Handler | ผ่านเมื่อ |
|---|---|---|
| 1 | `UserPermissionHandler` | บัญชีผู้ทำรายการไม่ได้มี `active = false` และมีสิทธิ์หากจองแทนผู้อื่น |
| 2 | `RoomAvailabilityHandler` | พบห้องที่ `AVAILABLE`; เก็บห้องใน `BookingValidationContext` ให้ขั้นถัดไปใช้ |
| 3 | `TimeOverlapHandler` | `startTime < endTime` และไม่ชน Booking ของห้องเดียวกันที่เป็น `PENDING` หรือ `APPROVED` โดยกรอง Booking เดิมออกตอนแก้ไข |
| 4 | `EquipmentAvailabilityHandler` | รวมจำนวนตาม `equipmentId` แล้วทุกชนิดมีจำนวนพอในช่วงที่ขอ |

`BookingValidationChain` รับ `List<BookingValidationHandler>` จาก Spring ซึ่งเรียงตาม `@Order` เมื่อ Handler โยน exception การวนหยุดทันที และ Service ไม่ไปถึงขั้น Mapper / สร้าง join rows / save

**การคิดจำนวนอุปกรณ์:** `available = totalQuantity - peakReserved` โดย `BookingEquipmentRepository.sumReservedQuantity()` คืนยอดที่ถูกจอง **พร้อมกันสูงสุด** ในช่วงที่ขอจาก Booking สถานะ `PENDING` / `APPROVED` ไม่ใช่รวมจำนวนทุก Booking ที่ทับช่วงนั้น การคำนวณใช้ `TreeMap` รวมการเพิ่มและคืนจำนวนตามเวลา

**การทับซ้อน:** ใช้ช่วง `[startTime, endTime)` และเงื่อนไข `existing.startTime < requested.endTime` พร้อม `existing.endTime > requested.startTime` การจองที่สิ้นสุดพอดีกับเวลาเริ่มรายการถัดไปจึงไม่ทับซ้อน การแก้ไขส่ง `excludeBookingId` ให้ query อุปกรณ์ไม่นับรายการเดิม

## Response body

ตัวอย่างต่อไปนี้เป็นข้อมูลสมมติเพื่อแสดงรูปแบบ `BookingResponse` ไม่ใช่ข้อมูลจากฐานข้อมูลจริง รายการอุปกรณ์ id 3 จำนวน 2 กับ 1 ถูกผูกเป็นจำนวน 3

```json
{
  "id": 101,
  "roomId": 1,
  "roomName": "Room A",
  "userId": 11,
  "username": "team_user",
  "startTime": "2026-10-20T09:00:00",
  "endTime": "2026-10-20T11:00:00",
  "status": "APPROVED",
  "purpose": "ประชุมทีมโปรเจกต์",
  "equipmentItems": [
    { "equipmentId": 3, "equipmentName": "Projector", "quantity": 3 }
  ],
  "createdAt": "2026-10-10T10:00:00"
}
```

`BookingMapper.toEntity()` สร้าง Booking เริ่มต้นที่ `PENDING` จากนั้น Service ใช้ Strategy ของคนที่ 2: ห้อง **STANDARD อนุมัติอัตโนมัติ → `APPROVED`**, ห้อง **VIP รออนุมัติ → `PENDING`** สถานะใน response จึงเป็นสถานะหลังผ่านขั้นตอนของ Service ไม่ใช่ค่าที่ client ส่งมา

Service เพิ่ม `BookingEquipment` ลง collection ของ Booking แล้วบันทึกด้วย `BookingRepository.save()` ผ่าน cascade ของ Booking ไม่ได้เรียก `BookingEquipmentRepository.save()` โดยตรง Mapper แปลง join entity กลับเป็นรายการ `equipmentId`, `equipmentName`, `quantity`; ถ้าไม่มีอุปกรณ์จะตอบ `equipmentItems: []`

## แก้ไขและแทนรายการอุปกรณ์

ตัวอย่าง PUT `/api/v1/bookings/101` เพื่อแก้เวลาและนำอุปกรณ์ทั้งหมดออก สมมติ Booking ยังเป็น `PENDING` ซึ่ง State ปัจจุบันอนุญาตให้แก้ไข

```json
{
  "roomId": 1,
  "startTime": "2026-10-20T10:00:00",
  "endTime": "2026-10-20T12:00:00",
  "purpose": "ปรับเวลาประชุมทีม",
  "equipmentItems": []
}
```

- Service ตรวจ `BookingContext.isEditable()` ก่อนเข้า validation chain ถ้าสถานะไม่อนุญาตจะแจ้ง 409; ใน State ปัจจุบัน `PENDING` แก้ไขได้ และ `APPROVED` แก้ไขไม่ได้
- Service เขียน `request.bookingId` เป็น id ที่โหลดจริง เพื่อไม่ให้การตรวจห้องและจำนวนอุปกรณ์ชนกับ Booking เดิม
- `BookingMapper.updateEntity()` เปลี่ยนเฉพาะห้อง, เวลาเริ่ม, เวลาสิ้นสุด และ purpose ไม่เปลี่ยน user หรือ status; การส่ง `bookingForUserId` ใน PUT จึงไม่ใช่การเปลี่ยนเจ้าของการจอง
- เมื่อผ่านการตรวจ Service ล้าง collection เดิมและเพิ่ม join rows ชุดใหม่ `orphanRemoval = true` ของ Booking ลบรายการเก่าตอน flush ใน flow นี้ไม่ได้เรียก `deleteByBookingId()`
- ถ้าไม่ส่ง `equipmentItems`, ส่ง `null` หรือส่ง `[]` ตอน PUT ที่สำเร็จ **รายการอุปกรณ์เดิมจะถูกล้างทั้งหมด** ไม่ใช่เก็บรายการเดิมไว้

## ตัวอย่าง error

Error ที่เกิดใน REST Controller / Service ใช้รูปแบบ `ErrorResponse` ของคนที่ 5 (`timestamp`, `status`, `error`, `message`, `path`, `details`) ตัวอย่างเวลาและ id ต่อไปนี้เป็นข้อมูลสมมติ

**400** ส่งจำนวนอุปกรณ์เป็น 0 ไป POST `/api/v1/bookings`

```json
{
  "timestamp": "2026-10-10T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "ข้อมูลที่ส่งมาไม่ถูกต้อง",
  "path": "/api/v1/bookings",
  "details": ["equipmentItems[0].quantity: จำนวนอุปกรณ์ต้องมากกว่า 0"]
}
```

**409** อุปกรณ์มีทั้งหมด 5 ชิ้น มีคนจองพร้อมกันสูงสุด 4 ชิ้น จึงเหลือ 1 ชิ้น แต่คำขอใหม่ต้องการ 3 ชิ้น

```json
{
  "timestamp": "2026-10-10T10:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "อุปกรณ์ 'Projector' ไม่เพียงพอ (ขอ 3 เหลือ 1)",
  "path": "/api/v1/bookings",
  "details": null
}
```

| Status | สาเหตุใน flow ที่เกี่ยวข้อง |
|---|---|
| 400 | DTO ไม่ผ่าน `@Valid`, JSON อ่านไม่ได้, เวลาเริ่มไม่ได้อยู่ก่อนเวลาสิ้นสุด, รายการอุปกรณ์หรือจำนวนไม่ถูกต้อง, รวมจำนวนซ้ำแล้วเกินขีดจำกัด `Integer` |
| 403 | ผู้ทำรายการถูกระงับ หรือไม่มีสิทธิ์จองแทนผู้อื่น; การบล็อก REST ของ profile `web` เป็นอีกชั้นก่อนเข้า Controller |
| 404 | ไม่พบผู้ทำรายการ, ห้อง, อุปกรณ์, Booking ที่แก้ไข หรือผู้รับการจองแทนตอนสร้าง |
| 409 | ห้องไม่พร้อมใช้, เวลาทับซ้อน, อุปกรณ์ไม่พอ หรือ State ไม่อนุญาตให้แก้ไข |

การไม่ผ่าน validation หมายถึงปฏิเสธคำขอก่อนบันทึก **ไม่ได้สร้าง Booking สถานะ `REJECTED`** ซึ่งเป็นคนละ flow ของ State Pattern

## ขอบเขตและข้อจำกัดที่ต้องทราบ

`UserPermissionHandler` ตรวจสิทธิ์จองแทนและบัญชีผู้ทำรายการ แต่ไม่ได้ตรวจว่าใครเป็นเจ้าของ Booking ใน PUT การตรวจสิทธิ์แก้ Booking ของผู้อื่นและการยืนยันตัวตน HTTP ต้องประสานงานคนที่ 1 / คนที่ 3 ไม่ควรอ้างว่า Handler นี้ครอบคลุม authorization ทั้งระบบ

การตรวจจำนวนแล้ว save ปัจจุบันไม่มี pessimistic lock หรือกลไกจองสต็อกแบบ atomic จึงยังไม่ใช่หลักฐานว่าคำขอหลายรายการพร้อมกันจะไม่แย่งจำนวนคงเหลือ การจัดเอกสารนี้ไม่ได้เปลี่ยนพฤติกรรมโค้ดดังกล่าว

## Diagram และโค้ดอ้างอิง

- [Sequence: สร้างสำเร็จ, แก้ไขสำเร็จ และอุปกรณ์ไม่พอ](diagrams/03-sequence-diagrams.md)
- [Class Diagram ของ Chain, DTO, Mapper และ Equipment Linking](diagrams/02-domain-and-class.md)
- [BookingCreateRequest.java](../../code/src/main/java/com/example/roombooking/dto/request/BookingCreateRequest.java), [BookingResponse.java](../../code/src/main/java/com/example/roombooking/dto/response/BookingResponse.java), [BookingMapper.java](../../code/src/main/java/com/example/roombooking/mapper/BookingMapper.java)
- [BookingValidationChain.java](../../code/src/main/java/com/example/roombooking/service/validation/BookingValidationChain.java), [BookingValidationContext.java](../../code/src/main/java/com/example/roombooking/service/validation/BookingValidationContext.java), [UserPermissionHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/UserPermissionHandler.java), [RoomAvailabilityHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/RoomAvailabilityHandler.java), [TimeOverlapHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/TimeOverlapHandler.java), [EquipmentAvailabilityHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/EquipmentAvailabilityHandler.java)
- [BookingEquipment.java](../../code/src/main/java/com/example/roombooking/domain/entity/BookingEquipment.java), [BookingEquipmentRepository.java](../../code/src/main/java/com/example/roombooking/repository/BookingEquipmentRepository.java)
- จุดเชื่อมต่อ: [BookingController.java](../../code/src/main/java/com/example/roombooking/controller/api/BookingController.java), [BookingServiceImpl.java](../../code/src/main/java/com/example/roombooking/service/impl/BookingServiceImpl.java), [GlobalExceptionHandler.java](../../code/src/main/java/com/example/roombooking/exception/GlobalExceptionHandler.java)
- ชั้น HTTP: [SecurityConfig.java](../../code/src/main/java/com/example/roombooking/config/SecurityConfig.java), [UserIdHeaderInterceptor.java](../../code/src/main/java/com/example/roombooking/config/UserIdHeaderInterceptor.java), [WebAccessInterceptor.java](../../code/src/main/java/com/example/roombooking/controller/web/support/WebAccessInterceptor.java)
