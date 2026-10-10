# API Documentation: ส่วน User และ Authentication

ผู้รับผิดชอบ: นายยุทธนา เหล่าวิสัย (673380422-8), branch `yuttana_673380422-8_03`  
Base URL: `/api/v1/users` (สำหรับ REST API บน profile `api`)  
Swagger UI: https://room-booking-api-k47v.onrender.com/swagger-ui.html

---

## 1. รายการ Endpoints ทั้งหมด

| Method | Endpoint | คำอธิบาย | Status Code สำเร็จ | Status Code ข้อผิดพลาด |
|---|---|---|:---:|:---:|
| `POST` | `/api/v1/users` | สร้างบัญชีผู้ใช้และโปรไฟล์ใหม่ | `201 Created` | `400 Bad Request` |
| `GET` | `/api/v1/users/{id}` | ดึงข้อมูลผู้ใช้ตาม ID | `200 OK` | `404 Not Found` |
| `GET` | `/api/v1/users` | ดึงรายการผู้ใช้ทั้งหมดแบบแบ่งหน้า (Pagination) | `200 OK` | `400 Bad Request` |
| `PUT` | `/api/v1/users/{id}` | แก้ไขข้อมูลผู้ใช้และโปรไฟล์ | `200 OK` | `400 Bad Request`, `404 Not Found` |
| `DELETE` | `/api/v1/users/{id}` | ลบบัญชีผู้ใช้และโปรไฟล์ | `204 No Content` | `404 Not Found` |

---

## 2. รายละเอียดแต่ละ Endpoint

### 2.1 สร้างผู้ใช้ใหม่ (Create User)
- **Method:** `POST`
- **URL:** `/api/v1/users`
- **Request Headers:** `Content-Type: application/json`

#### Request Body (`UserCreateRequest`):
```json
{
  "username": "somchai_it",
  "email": "somchai@example.com",
  "password": "SecurePassword123!",
  "role": "USER",
  "fullName": "สมชาย ใจดี",
  "phone": "0812345678",
  "department": "Information Technology"
}
```

#### กฎการตรวจสอบข้อมูล (Validation Rules):
| ฟิลด์ | เงื่อนไข | ข้อความเมื่อผิดพลาด |
|---|---|---|
| `username` | `@NotBlank`, ความยาว 4–50 ตัวอักษร | "Username is required", "Username must be between 4 and 50 characters" |
| `email` | `@NotBlank`, รูปแบบอีเมลถูกต้อง | "Email is required", "Invalid email format" |
| `password` | `@NotBlank`, ความยาวขั้นต่ำ 6 ตัวอักษร | "Password is required", "Password must be at least 6 characters long" |
| `role` | `@NotNull`, ค่าที่เป็นไปได้: `USER`, `STAFF`, `ADMIN` | "Role is required" |
| `fullName` | ตัวอักษรทั่วไป (Optional) | - |
| `phone` | ตัวอักษรทั่วไป (Optional) | - |
| `department` | ตัวอักษรทั่วไป (Optional) | - |

#### Response (`201 Created`):
```json
{
  "id": 1,
  "username": "somchai_it",
  "email": "somchai@example.com",
  "role": "USER",
  "fullName": "สมชาย ใจดี",
  "phone": "0812345678",
  "department": "Information Technology",
  "createdAt": "2026-10-10T10:00:00"
}
```

---

### 2.2 ดึงข้อมูลผู้ใช้ตาม ID (Get User by ID)
- **Method:** `GET`
- **URL:** `/api/v1/users/{id}`
- **Path Variable:** `id` (Long)

#### Response (`200 OK`):
```json
{
  "id": 1,
  "username": "somchai_it",
  "email": "somchai@example.com",
  "role": "USER",
  "fullName": "สมชาย ใจดี",
  "phone": "0812345678",
  "department": "Information Technology",
  "createdAt": "2026-10-10T10:00:00"
}
```

#### กรณีไม่พบข้อมูล (`404 Not Found`):
```json
{
  "timestamp": "2026-10-10T10:05:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: 99",
  "path": "/api/v1/users/99"
}
```

---

### 2.3 ดึงรายการผู้ใช้ทั้งหมดแบบแบ่งหน้า (Get All Users with Pagination)
- **Method:** `GET`
- **URL:** `/api/v1/users?page=0&size=10`
- **Query Parameters:**
  - `page`: หน้าที่ต้องการ (เริ่มที่ 0, Default: 0)
  - `size`: จำนวนรายการต่อหน้า (Default: 10)
  - *ระบบจะทำการเรียงลำดับตาม `id,desc` โดยอัตโนมัติ*

#### Response (`200 OK`):
```json
{
  "content": [
    {
      "id": 2,
      "username": "admin_user",
      "email": "admin@example.com",
      "role": "ADMIN",
      "fullName": "ผู้ดูแลระบบ สูงสุด",
      "phone": "0899999999",
      "department": "Management",
      "createdAt": "2026-10-10T09:30:00"
    },
    {
      "id": 1,
      "username": "somchai_it",
      "email": "somchai@example.com",
      "role": "USER",
      "fullName": "สมชาย ใจดี",
      "phone": "0812345678",
      "department": "Information Technology",
      "createdAt": "2026-10-10T09:00:00"
    }
  ],
  "pageNo": 0,
  "pageSize": 10,
  "totalElements": 2,
  "totalPages": 1,
  "last": true
}
```

---

### 2.4 แก้ไขข้อมูลผู้ใช้ (Update User)
- **Method:** `PUT`
- **URL:** `/api/v1/users/{id}`
- **Path Variable:** `id` (Long)

#### Request Body (`UserUpdateRequest`):
*(ฟิลด์ทั้งหมดเป็น Optional ฟิลด์ที่เป็น `null` จะไม่ถูกแก้ไขทับค่าเดิม)*
```json
{
  "email": "somchai.new@example.com",
  "fullName": "สมชาย ใจงาม",
  "phone": "0898765432",
  "department": "Software Engineering"
}
```

#### Response (`200 OK`):
```json
{
  "id": 1,
  "username": "somchai_it",
  "email": "somchai.new@example.com",
  "role": "USER",
  "fullName": "สมชาย ใจงาม",
  "phone": "0898765432",
  "department": "Software Engineering",
  "createdAt": "2026-10-10T10:00:00"
}
```

---

### 2.5 ลบผู้ใช้ (Delete User)
- **Method:** `DELETE`
- **URL:** `/api/v1/users/{id}`
- **Path Variable:** `id` (Long)

#### Response (`204 No Content`):
- คืนค่า status 204 โดยไม่มี Response Body
- ข้อมูล `UserProfile` ที่ผูกอยู่จะถูกลบไปด้วยผ่าน JPA `orphanRemoval = true`

#### กรณีไม่พบ ID ที่ต้องการลบ (`404 Not Found`):
```json
{
  "timestamp": "2026-10-10T10:15:00",
  "status": 404,
  "error": "Not Found",
  "message": "Cannot delete. User not found with id: 99",
  "path": "/api/v1/users/99"
}
```
