# API: Room และ Equipment

Base URL: `/api/v1` ทุก error ตอบเป็นรูปแบบ `ErrorResponse` เดียวกัน (`timestamp`, `status`, `error`, `message`, `path`, `details`)

## Room: `/api/v1/rooms`

| Method | Path | ทำอะไร | สำเร็จ | Error |
|---|---|---|---|---|
| POST | `/api/v1/rooms` | สร้างห้อง | 201 | 400 ข้อมูลไม่ผ่าน validation |
| GET | `/api/v1/rooms/{id}` | ดูห้อง | 200 | 404 ไม่พบห้อง |
| GET | `/api/v1/rooms?page=0&size=10&sort=name,asc` | รายการห้อง (pagination + sorting) | 200 | |
| PUT | `/api/v1/rooms/{id}` | แก้ห้อง | 200 | 400, 404 |
| DELETE | `/api/v1/rooms/{id}` | ลบห้อง | 204 | 404, **409 มีการจองผูกอยู่** |

**Request body**

```json
{ "name": "Room A", "capacity": 10, "floor": "2", "roomType": "STANDARD", "status": "AVAILABLE" }
```

| Field | กฎ |
|---|---|
| `name` | ห้ามว่าง, ไม่เกิน 100 ตัวอักษร |
| `capacity` | ห้าม null, ต้องมากกว่า 0 |
| `floor` | ไม่บังคับ, ไม่เกิน 20 ตัวอักษร |
| `roomType` | ห้าม null, `STANDARD` หรือ `VIP` |
| `status` | ห้าม null, `AVAILABLE` หรือ `MAINTENANCE` |

`roomType` กำหนดกฎการจอง: **STANDARD อนุมัติอัตโนมัติ**, **VIP ต้องรอ admin อนุมัติ**

## Equipment: `/api/v1/equipments`

| Method | Path | ทำอะไร | สำเร็จ | Error |
|---|---|---|---|---|
| POST | `/api/v1/equipments` | เพิ่มอุปกรณ์ | 201 | 400 |
| GET | `/api/v1/equipments/{id}` | ดูอุปกรณ์ | 200 | 404 |
| GET | `/api/v1/equipments?page=0&size=10` | รายการอุปกรณ์ (pagination) | 200 | |
| PUT | `/api/v1/equipments/{id}` | แก้อุปกรณ์ | 200 | 400, 404 |
| DELETE | `/api/v1/equipments/{id}` | ลบอุปกรณ์ | 204 | 404, **409 ถูกใช้ในการจอง** |

**Request body**

```json
{ "name": "Projector", "totalQuantity": 5, "category": "Display" }
```

| Field | กฎ |
|---|---|
| `name` | ห้ามว่าง, ไม่เกิน 100 ตัวอักษร |
| `totalQuantity` | ห้าม null, ห้ามติดลบ |
| `category` | ไม่บังคับ, ไม่เกิน 50 ตัวอักษร |

## ตัวอย่าง error

**400** ส่ง `{"name":"","capacity":0}` ไป `POST /api/v1/rooms`

```json
{ "status": 400, "error": "Bad Request", "message": "...", "path": "/api/v1/rooms",
  "details": ["name: name is required", "capacity: capacity must be greater than 0", "..."] }
```

**409** ลบห้องที่ยังมีการจอง

```json
{ "status": 409, "error": "Conflict",
  "message": "ไม่สามารถลบห้องได้ เพราะมีการจองที่เกี่ยวข้องกับห้องนี้อยู่ (id: 1)",
  "path": "/api/v1/rooms/1" }
```
