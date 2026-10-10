# API: Booking

Base path `/api/v1` ทุก endpoint อยู่ใน `controller/api/BookingController.java`

> REST API เปิดเมื่อรันด้วย profile `api` (`.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=api"`) โหมด `prod` ของเว็บปิด `/api/**` ไว้ด้วย 403 ตาม `HOWTORUN.md` Swagger UI อยู่ที่ `/swagger-ui.html`

## Endpoint

| Method | URL | ทำอะไร | สำเร็จ | Header |
|---|---|---|---|---|
| POST | `/api/v1/bookings` | สร้างการจอง | 201 Created | `X-User-Id` (บังคับ) |
| GET | `/api/v1/bookings/{id}` | ดูการจอง 1 รายการ | 200 OK | |
| GET | `/api/v1/rooms/{roomId}/bookings` | การจองของห้อง แบ่งหน้า | 200 OK | |
| GET | `/api/v1/users/{userId}/bookings` | การจองของผู้ใช้ แบ่งหน้า | 200 OK | |
| PUT | `/api/v1/bookings/{id}` | แก้ไขการจอง (เฉพาะ PENDING) | 200 OK | `X-User-Id` (บังคับ) |
| PATCH | `/api/v1/bookings/{id}/status` | เปลี่ยนสถานะ | 200 OK | `X-User-Id` |

## Request

### สร้างหรือแก้ไขการจอง (POST, PUT)

```json
{
  "roomId": 2,
  "bookingForUserId": null,
  "startTime": "2030-01-01T09:00:00",
  "endTime": "2030-01-01T11:00:00",
  "purpose": "Sprint planning",
  "equipmentItems": [
    { "equipmentId": 10, "quantity": 2 }
  ]
}
```

| Field | บังคับ | หมายเหตุ |
|---|---|---|
| `roomId` | ใช่ | |
| `bookingForUserId` | ไม่ | จองแทนผู้ใช้อื่น ต้องเป็น ADMIN หรือ STAFF ไม่ใส่ = จองให้ตัวเอง |
| `startTime`, `endTime` | ใช่ | ISO 8601 `startTime` ต้องก่อน `endTime` |
| `purpose` | ไม่ | |
| `equipmentItems` | ไม่ | อุปกรณ์ชิ้นเดียวกันส่งซ้ำได้ ระบบรวมจำนวนให้ |
| `bookingId` | ไม่ต้องส่ง | ระบบเขียนทับเอง (null ตอนสร้าง, id การจองตอนแก้ไข) |

### เปลี่ยนสถานะ (PATCH)

```json
{ "status": "APPROVED", "reason": "optional" }
```

`status` เป็นสถานะปลายทาง: `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED` (ส่ง `PENDING` ได้ 400)

## Response

```json
{
  "id": 100,
  "roomId": 2,
  "roomName": "VIP Room",
  "userId": 1,
  "username": "sol",
  "startTime": "2030-01-01T09:00:00",
  "endTime": "2030-01-01T11:00:00",
  "status": "PENDING",
  "purpose": "Sprint planning",
  "equipmentItems": [
    { "equipmentId": 10, "equipmentName": "Projector", "quantity": 2 }
  ],
  "createdAt": "2029-12-20T14:30:00"
}
```

ห้อง STANDARD ได้ `status` เป็น `APPROVED` ทันที ห้อง VIP ได้ `PENDING`

## Status code

| Code | เมื่อไหร่ | ตัวอย่าง |
|---|---|---|
| 200 / 201 | สำเร็จ | |
| 400 | ข้อมูลไม่ครบหรือผิดรูปแบบ, ไม่มี `X-User-Id`, เวลาเริ่มไม่ก่อนเวลาจบ, เปลี่ยนกลับเป็น PENDING, sort ด้วย field ที่ไม่มี | `startTime` ว่าง |
| 403 | บัญชีถูกระงับ หรือจองแทนผู้อื่นโดยไม่มีสิทธิ์ | USER ส่ง `bookingForUserId` ของคนอื่น |
| 404 | ไม่พบผู้ใช้ ห้อง อุปกรณ์ หรือการจอง | `GET /api/v1/bookings/999` |
| 409 | ห้องไม่ว่าง, เวลาซ้อน, อุปกรณ์ไม่พอ, เปลี่ยนสถานะหรือแก้ไขผิดกฎ | ยกเลิกการจองที่ COMPLETED |

Error ทุกตัวใช้รูปแบบ `ErrorResponse` เดียวกัน (`timestamp`, `status`, `error`, `message`, `path`, `details`) จาก `GlobalExceptionHandler`

## Pagination และ Sorting

```
GET /api/v1/rooms/3/bookings?page=0&size=10&sort=startTime,desc
```

| Parameter | ค่าเริ่มต้น | หมายเหตุ |
|---|---|---|
| `page` | 0 | เริ่มนับที่ 0 |
| `size` | 10 | |
| `sort` | `startTime` (น้อยไปมาก) | `field,asc` หรือ `field,desc` |

`Pageable` ส่งตรงถึง `BookingRepository` ฐานข้อมูลทำ `LIMIT`, `OFFSET` และ `ORDER BY` เอง ไม่ได้ดึงทั้งหมดมาตัดใน Java

## ตัวอย่างลำดับเรียก (ใช้ demo)

1. `POST /api/v1/bookings` ห้อง VIP ได้ 201 สถานะ PENDING
2. `POST` ห้องเดิมเวลาทับกัน ได้ 409
3. `PATCH .../status` `APPROVED` ได้ 200
4. `PATCH .../status` `COMPLETED` ได้ 200
5. `PATCH .../status` `CANCELLED` ได้ 409 `Cannot cancel a booking in status COMPLETED`
6. `GET /api/v1/rooms/{id}/bookings?page=0&size=2&sort=startTime,desc`

## Sequence Diagram

| Endpoint | Diagram |
|---|---|
| `POST /api/v1/bookings` | [diagrams/sequence-create-booking.md](diagrams/sequence-create-booking.md) |
| `PUT /api/v1/bookings/{id}` | [diagrams/sequence-update-booking.md](diagrams/sequence-update-booking.md) |
| `PATCH /api/v1/bookings/{id}/status` | [diagrams/sequence-update-status.md](diagrams/sequence-update-status.md) |
