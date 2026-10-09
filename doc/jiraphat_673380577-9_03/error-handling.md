# Error Handling และ API Documentation

ผู้รับผิดชอบ: นายจีรภัทร แก้วดี (673380577-9), branch `jiraphat_673380577-9_03`
Path ย่อจาก `code/src/main/java/com/example/roombooking/`

## หลักการ

- **จัดการที่เดียว:** exception ทุกตัวที่หลุดจาก REST controller ถูกจับที่ `exception/GlobalExceptionHandler.java` แล้วแปลงเป็น HTTP status ที่ถูกต้อง พร้อม JSON รูปแบบเดียวกัน
- **ขอบเขต:** `@RestControllerAdvice(basePackages = "com.example.roombooking.controller.api")` (บรรทัด 27) จับเฉพาะ REST API หน้าเว็บ Thymeleaf (`controller.web`) ใช้ error page ของตัวเอง (`WebHttpErrorAdvice`) จึงไม่ได้รับ JSON กลับไป
- **ไม่เปิดเผยรายละเอียดภายใน:** error จากฐานข้อมูลและ error ที่ไม่คาดคิด จะตอบเป็นข้อความกลาง ๆ ส่วนรายละเอียดเขียนลง log ฝั่ง server เท่านั้น

## รูปแบบ Error Response

`dto/response/ErrorResponse.java`

```json
{
  "timestamp": "2026-10-09T15:34:10.435",
  "status": 400,
  "error": "Bad Request",
  "message": "ข้อมูลที่ส่งมาไม่ถูกต้อง",
  "path": "/api/v1/users",
  "details": ["username: Username must be between 4 and 50 characters"]
}
```

| Field | ความหมาย |
|---|---|
| `timestamp` | เวลาที่เกิด error |
| `status` | HTTP status code |
| `error` | ชื่อ status (เช่น `Not Found`) |
| `message` | ข้อความสำหรับผู้ใช้ |
| `path` | URL ที่เรียก |
| `details` | รายการ field ที่ไม่ผ่าน validation (มีเฉพาะ 400 จาก `@Valid`) |

## ตาราง Exception → HTTP Status

| Exception | Status | ตัวอย่างสถานการณ์ | บรรทัด |
|---|---|---|---|
| `ResourceNotFoundException` | **404** | `GET /api/v1/rooms/99999` | 33–38 |
| `RoomNotAvailableException` | **409** | จองห้องช่วงเวลาที่ทับกับการจองอื่น | 41–46 |
| `EquipmentNotAvailableException` | **409** | ขออุปกรณ์เกินจำนวนที่ว่าง | 49–54 |
| `InvalidStateTransitionException` | **409** | อนุมัติการจองที่ถูกยกเลิกไปแล้ว (State Pattern) | 57–62 |
| `ConflictException` | **409** | ลบห้องที่ยังมีการจองอยู่ | 65–69 |
| `DataIntegrityViolationException` | **409** | ฐานข้อมูลปฏิเสธเพราะผิด constraint โดยไม่ส่งข้อความ SQL กลับไป | 73–79 |
| `ForbiddenException` | **403** | ผู้ใช้ไม่มีสิทธิ์ทำรายการ | 82–87 |
| `MethodArgumentNotValidException` | **400** + `details` | body ไม่ผ่าน `@Valid` เช่น username สั้นไป | 90–98 |
| `PropertyReferenceException` / `InvalidDataAccessApiUsageException` | **400** | `?sort=string` (ค่าตัวอย่างใน Swagger) หรือเรียงด้วย field ที่ไม่มีจริง | 101–117 |
| `HttpMessageNotReadableException` | **400** | JSON พัง, ค่า enum ไม่มีจริง, รูปแบบวันที่ผิด | 120–125 |
| `MethodArgumentTypeMismatchException` | **400** | `GET /api/v1/rooms/abc` | 128–134 |
| `IllegalArgumentException` | **400** | เปลี่ยนสถานะกลับเป็น `PENDING` | 137–142 |
| exception ของ Spring MVC (`org.springframework.web.ErrorResponse`) | ใช้ status ของตัวเอง | 405 method ผิด, 404 ไม่มี path | 149–155 |
| `Exception` อื่น ๆ | **500** | error ที่ไม่คาดคิด ตอบข้อความกลาง ๆ และไม่ส่ง stack trace กลับไป | 145–159 |

ลำดับการจับ: Spring เลือก handler ที่ตรงกับชนิดของ exception มากที่สุดก่อน `Exception` (บรรทัด 145) จึงเป็นตัวสุดท้ายเสมอ

## ปัญหาที่แก้ระหว่างทำ

| ปัญหา | สาเหตุ | วิธีแก้ |
|---|---|---|
| `?sort=string` ใน Swagger ได้ 500 | Spring Data โยน `PropertyReferenceException` ห่อใน `InvalidDataAccessApiUsageException` แล้วไปตกที่ตัวจับ 500 | เพิ่ม handler บรรทัด 100–117 ให้ตอบ 400 พร้อมบอกชื่อ field (test: `InvalidSortIntegrationTest`) |
| 405 / 404 ของ Spring กลายเป็น 500 | ตัวจับ `Exception` จับทุกอย่าง | ตรวจ `instanceof org.springframework.web.ErrorResponse` แล้วใช้ status เดิม (บรรทัด 149–155) |
| ลบข้อมูลที่ถูกอ้างอิงแล้ว error หลุดชื่อตาราง | ข้อความจาก PostgreSQL ถูกส่งกลับไปตรง ๆ | ตอบข้อความกลาง ๆ แล้ว log รายละเอียดฝั่ง server (บรรทัด 73–79) |
| หน้าเว็บได้ JSON แทนหน้า error | Advice จับทุก controller | จำกัด `basePackages` ไว้ที่ `controller.api` (บรรทัด 27) |

## Swagger / OpenAPI

| | |
|---|---|
| Library | `springdoc-openapi-starter-webmvc-ui` 3.1.0 (รองรับ Spring Boot 4) |
| Config | `config/SwaggerConfig.java` บรรทัด 14–20: ชื่อ "Room Booking API" เวอร์ชัน 1.0.0 |
| เปิดเมื่อ | profile `api` เท่านั้น ส่วน profile `prod` (หน้าเว็บ) ปิด Swagger ไว้ |
| URL จริง | https://room-booking-api-k47v.onrender.com/swagger-ui.html |
| OpenAPI JSON | https://room-booking-api-k47v.onrender.com/v3/api-docs |

`SwaggerConfig` ไม่ได้ประกาศ security scheme (เช่น JWT) เพราะ REST API ยังไม่มีการยืนยันตัวตนจริง ผู้ใช้ถูกระบุด้วย header `X-User-Id` ซึ่ง springdoc แสดงเป็น parameter ของแต่ละ endpoint ให้อยู่แล้ว ถ้าประกาศ scheme ที่ระบบไม่ได้ใช้จริง Swagger จะแสดงปุ่ม Authorize ที่กดแล้วไม่มีผล

## ทดสอบ

| Test | ยืนยันอะไร |
|---|---|
| `exception/GlobalExceptionHandlerTest` (16 test) | แต่ละ exception ได้ status ถูกต้อง (400/403/404/405/409/500), JSON มีครบทุก field, ไม่หลุดข้อความ SQL หรือรายละเอียดภายใน, controller หน้าเว็บไม่ถูกจับเป็น JSON |
| `exception/InvalidSortIntegrationTest` (5 test) | `?sort=string` ที่ rooms, equipments, rooms/{id}/bookings ได้ 400 และ sort ด้วย field จริงยังใช้ได้ |
| `config/SwaggerConfigTest` (3 test) | `/v3/api-docs` มีข้อมูล API และ endpoint ของ booking, ไม่ประกาศระบบ login ที่ยังไม่มี, Swagger UI เปิดได้ |
