# ส่วนงานคนที่ 1: User, UserProfile, Authentication และสิทธิ์ผู้ใช้

| | |
|---|---|
| **ชื่อ-นามสกุล** | นายยุทธนา เหล่าวิสัย |
| **รหัสนักศึกษา** | 673380422-8 |
| **Section** | 03 |
| **Branch** | `yuttana_673380422-8_03` |
| **หน้าที่รับผิดชอบ** | จัดการบัญชีผู้ใช้ (User) และโปรไฟล์ (UserProfile) แบบ One-to-One, User Controller/Service/Repository, เข้ารหัสรหัสผ่าน (BCrypt), ระบบ Login, Session และสิทธิ์ผู้ใช้ (Role: USER, STAFF, ADMIN) |

---

## เอกสารในโฟลเดอร์นี้

| ไฟล์ | เนื้อหา |
|---|---|
| [solid-analysis.md](solid-analysis.md) | การวิเคราะห์หลักการ SOLID ทั้ง 5 ข้อของส่วน User & Authentication พร้อมระบุไฟล์และบรรทัด |
| [design-patterns.md](design-patterns.md) | Design Patterns ที่ใช้: Layered Architecture, DTO + Mapper, Repository Pattern, Dependency Injection |
| [api-user-auth.md](api-user-auth.md) | สเปก REST API ของ `/api/v1/users` (CRUD), Request/Response DTO, Validation และ Status Codes |
| [diagrams/class-user-profile.md](diagrams/class-user-profile.md) | Class Diagram แสดงโครงสร้าง User, UserProfile, Role, DTOs, Mapper, Service, Controller |
| [diagrams/sequence-create-user.md](diagrams/sequence-create-user.md) | Sequence Diagram การสร้างผู้ใช้ใหม่และการเข้ารหัสผ่าน BCrypt |
| [diagrams/sequence-user-login.md](diagrams/sequence-user-login.md) | Sequence Diagram การยืนยันตัวตน เข้าสู่ระบบ และการตรวจสอบสิทธิ์ผ่าน WebSessionSupport |
| [diagrams/er-user-profile.md](diagrams/er-user-profile.md) | ER Diagram ความสัมพันธ์แบบ 1-to-1 ระหว่างตาราง `users` และ `user_profile` |

---

## โค้ดที่รับผิดชอบ

```
code/src/main/java/com/example/roombooking/
├── domain/
│   ├── entity/
│   │   ├── User.java                     # Entity หลักของผู้ใช้ (id, username, email, password, role, active, created_at)
│   │   └── UserProfile.java              # Entity โปรไฟล์ 1-to-1 (fullName, phone, department, user_id)
│   └── enums/
│       └── Role.java                     # Enum กำหนดสิทธิ์: USER, STAFF, ADMIN
├── repository/
│   └── UserRepository.java               # Spring Data JPA Repository (findByUsername, existsById)
├── service/
│   ├── UserService.java                  # Interface สัญญางาน business logic
│   └── impl/
│       └── UserServiceImpl.java          # Implementation จัดการ CRUD, เข้ารหัส BCrypt, แบ่งหน้า
├── mapper/
│   └── UserMapper.java                   # แปลงข้อมูลไป-กลับระหว่าง User Entity และ DTO
├── dto/
│   ├── request/
│   │   ├── UserCreateRequest.java        # DTO รับข้อมูลสร้างผู้ใช้ พร้อม Bean Validation
│   │   └── UserUpdateRequest.java        # DTO รับข้อมูลอัปเดตโปรไฟล์และอีเมล
│   └── response/
│       └── UserResponse.java             # DTO ส่งข้อมูลผู้ใช้ออกสู่ภายนอก (ซ่อน password)
├── controller/
│   ├── api/
│   │   └── UserController.java           # REST Controller (/api/v1/users) รองรับ CRUD และ Pagination
│   └── web/
│       ├── AuthViewController.java       # Web Controller จัดการ /login, /register, /logout
│       └── support/
│           └── WebSessionSupport.java    # คลาสสนับสนุน Session, ตรวจสอบสิทธิ์ (requireUser, requireManager, requireAdmin)
└── config/
    ├── SecurityConfig.java               # Spring Security สำหรับ Profile API (BCrypt 12, permitAll สำหรับ REST)
    └── WebSecurityConfig.java            # Spring Security สำหรับ Profile Web
```

---

## การทดสอบ (Tests)

ชุดทดสอบของส่วนนี้ตั้งอยู่ที่:
`test/yuttana_673380422-8_03/java/` (และ shared controller tests ใน `code/src/test/java/`)

| ชุดทดสอบ | ไฟล์ | จำนวนเคส | ขอบเขตการทดสอบ |
|---|---|:---:|---|
| **Service Layer** | `test/.../service/impl/UserServiceImplTest.java` | 11 | Mockito Unit Test: ทดสอบ `getUserById`, `createUser`, `updateUser`, `getAllUsers` (Pagination), `exitsById`, `deleteUserById` ทั้งเคสปกติและ ResourceNotFoundException (404) |
| **API Controller** | `code/src/test/.../controller/api/UserControllerTest.java` | 6 | Mockito Unit Test: ทดสอบ HTTP Status (201 Created, 200 OK, 204 No Content), การแปลง Pageable, และค่า Response DTO |
| **Web Auth Flow** | `code/src/test/.../controller/web/WebAuthFlowTest.java` | 6 | MockMvc Test: ทดสอบการ redirect เมื่อยังไม่ login, การเช็กรหัสผ่าน BCrypt, CSRF token guard, และการตรวจสอบสิทธิ์ตาม Role |

### วิธีรันการทดสอบ

1. **รันเฉพาะชุดเทสต์ของยุทธนา (Profile):**
   ```bash
   cd code
   ./mvnw clean test -P yuttana_673380422-8_03
   ```
   *(ผลการรัน: ผ่านครบทุก 11 tests โดยรายงานจะถูกสร้างไว้ที่ `code/target/yuttana_673380422-8_03-surefire-reports/`)*

2. **รันรวมทั้งโปรเจกต์ (รวมของเพื่อนทุกคน):**
   ```bash
   cd code
   ./mvnw clean test
   ```

---

## จุดเด่นและการออกแบบทางเทคนิค

1. **One-to-One Bidirectional Mapping with Cascade:**
   - ตาราง `users` และ `user_profile` เชื่อมกันด้วยความสัมพันธ์ 1-to-1 โดย `user_profile` มี foreign key `user_id` (unique constraint)
   - ที่ `User.java` ใช้ `cascade = CascadeType.ALL, orphanRemoval = true` ทำให้เมื่อบันทึกหรือลบ User ระบบจะจัดการ UserProfile ให้โดยอัตโนมัติ
   - Method `setProfile()` และ `setUser()` ผูกความสัมพันธ์กลับทั้งสองฝั่งโดยอัตโนมัติ ป้องกันปัญหา Inconsistent State

2. **ความปลอดภัยของรหัสผ่าน (Password Security):**
   - รหัสผ่านถูกเข้ารหัสด้วย `BCryptPasswordEncoder(12)` (Strength Cost Factor 12) ทันทีใน `UserServiceImpl.createUser()` ก่อนบันทึกลงฐานข้อมูล
   - ใน `UserResponse` ไม่มีการส่งฟิลด์ `password` กลับออกไปเด็ดขาด เพื่อป้องกันการรั่วไหลของข้อมูลความลับ

3. **Layered Architecture & Data Encapsulation:**
   - Controller ไม่สัมผัส Entity หรือ Repository โดยตรง แต่สื่อสารผ่าน DTO (`UserCreateRequest`, `UserUpdateRequest`, `UserResponse`) และ `UserService`
   - การอัปเดตข้อมูลใน `updateUser()` อัปเดตเฉพาะฟิลด์ที่ไม่เป็น `null` และไม่เปิดให้แก้ username/password ผ่าน endpoint ทั่วไป

---

## คำถามและแนวทางการตอบตอนนำเสนอ

**1. ทำไมต้องแยก Entity เป็น `User` และ `UserProfile` แทนที่จะรวมเป็นตารางเดียว?**
> **ตอบ:** เพื่อยึดหลัก **Single Responsibility Principle (SRP)** และ **Separation of Concerns** โดย `User` ดูแลข้อมูลเกี่ยวกับการยืนยันตัวตนและความปลอดภัย (Authentication & Account State เช่น username, password, role, active) ส่วน `UserProfile` ดูแลข้อมูลส่วนบุคคลทั่วไป (fullName, phone, department) การแยกนี้ทำให้โครงสร้างยืดหยุ่น หากในอนาคตต้องการขยายโปรไฟล์หรือเพิ่ม Social Login ก็ไม่กระทบกับตารางบัญชีหลัก

**2. ใน Entity `User` และ `UserProfile` เชื่อมกันอย่างไร และจัดการ Consistency อย่างไร?**
> **ตอบ:** เชื่อมกันแบบ `@OneToOne` โดยฝั่ง `UserProfile` เป็นเจ้าของความสัมพันธ์ (Owning Side) มี `@JoinColumn(name = "user_id", unique = true)` และฝั่ง `User` ใช้ `mappedBy = "user"` พร้อม `cascade = CascadeType.ALL` นอกจากนี้ใน method `user.setProfile(profile)` และ `profile.setUser(user)` มีโค้ดตรวจสอบและตั้งค่าอ้างอิงกลับ (Bi-directional link) ให้อัตโนมัติ

**3. การเข้ารหัสรหัสผ่านทำงานที่จุดไหน และทำไมใช้ BCrypt Strength 12?**
> **ตอบ:** เข้ารหัสใน `UserServiceImpl.createUser()` ผ่าน `PasswordEncoder.encode()` ก่อนที่จะนำ Entity ไป `save()` และในระบบใช้ `BCryptPasswordEncoder(12)` ซึ่งเป็นระดับมาตรฐานความปลอดภัยที่แนะนำใน Production โดยมี Salt ในตัวและทนทานต่อการ Brute-force/Rainbow Table

**4. ทำไม `UserResponse` ถึงไม่คืน password ออกไป?**
> **ตอบ:** เป็นไปตามหลัก Information Hiding และ Data Privacy โดยการใช้ DTO (`UserResponse`) แทนที่จะส่ง `User` Entity ออกไปตรงๆ ทำให้เราคัดกรองเฉพาะข้อมูลที่จำเป็นและปลอดภัยต่อ Client ได้เท่านั้น รหัสผ่านแม้จะถูก hash แล้วก็ไม่ควรส่งออกทาง API
