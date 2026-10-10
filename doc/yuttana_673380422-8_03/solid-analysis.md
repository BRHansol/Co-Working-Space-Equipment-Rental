# SOLID Analysis: ส่วน User, UserProfile และ Authentication

ผู้รับผิดชอบ: นายยุทธนา เหล่าวิสัย (673380422-8), branch `yuttana_673380422-8_03`  
Path ในตารางย่อจาก `code/src/main/java/com/example/roombooking/`

---

## S: Single Responsibility Principle (SRP)

คลาสแต่ละคลาสควรมีความรับผิดชอบเพียงอย่างเดียว และมีเหตุผลในการเปลี่ยนแปลงเพียงเหตุผลเดียว

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `controller/api/UserController.java` | 20–66 | จัดการ HTTP Request/Response, แปลง `@Valid` และคืน Status Code (201, 200, 204) เท่านั้น ไม่มี business logic และไม่เรียก Repository โดยตรง |
| `dto/request/UserCreateRequest.java` | 11–33 | กำหนดโครงสร้างข้อมูลขาเข้าและกฎ Validation ด้วย Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Email`) แยกจาก Service |
| `dto/response/UserResponse.java` | 10–25 | กำหนดโครงสร้างข้อมูลขาออก ป้องกันการหลุดของฟิลด์ `password` ออกสู่ภายนอก |
| `service/impl/UserServiceImpl.java` | 21–82 | จัดการ Business Logic เกี่ยวกับบัญชีผู้ใช้ (ตรวจสอบข้อมูล, เรียก encode รหัสผ่าน, จัดการแบ่งหน้า PageResponse, จัดการ Exception 404) |
| `mapper/UserMapper.java` | 13–80 | ทำหน้าที่แปลงข้อมูล (Mapping) ระหว่าง Entity ↔ DTO เพียงอย่างเดียว แยกความซับซ้อนของการแมปออกจาก Service |
| `domain/entity/User.java` | 18–120 | ดูแลข้อมูลบัญชีผู้ใช้และสิทธิ์การเข้าถึง (`username`, `email`, `password`, `role`, `active`) |
| `domain/entity/UserProfile.java` | 9–78 | ดูแลข้อมูลส่วนบุคคล (`fullName`, `phone`, `department`) แยกออกจากตารางบัญชีหลัก |
| `config/SecurityConfig.java` | 15–54 | กำหนดค่านโยบายความปลอดภัยและสร้าง Bean `PasswordEncoder` แยกออกจากการทำงานส่วนอื่น |

---

## O: Open/Closed Principle (OCP)

ซอฟต์แวร์ควรเปิดให้ขยายการทำงาน (Open for Extension) แต่ปิดต่อการแก้ไขโค้ดเดิม (Closed for Modification)

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `service/UserService.java` | 8–15 | กำหนดเป็น Interface สัญญางานกลาง หากต้องการเปลี่ยนวิธีจัดการผู้ใช้ (เช่น เพิ่ม `LdapUserServiceImpl` หรือ `OAuthUserServiceImpl`) สามารถสร้างคลาสใหม่มา implement ได้ทันทีโดยไม่ต้องแก้ `UserController` |
| `service/impl/UserServiceImpl.java` | 28, 40 | พึ่งพา `PasswordEncoder` ซึ่งเป็น Interface ของ Spring Security หากต้องการเปลี่ยนอัลกอริทึมเข้ารหัส (เช่น จาก BCrypt เป็น Argon2) สามารถเปลี่ยนที่ Config Bean ได้ทันทีโดยไม่ต้องแก้โค้ดใน `UserServiceImpl` |
| `mapper/UserMapper.java` | 41–60 | Method `updateEntity` ตรวจสอบ null ก่อนอัปเดต ทำให้รองรับการขยายฟิลด์ในอนาคตได้อย่างปลอดภัยโดยไม่กระทบฟิลด์เดิม |

---

## L: Liskov Substitution Principle (LSP)

คลาสลูกหรือคลาสที่ Implement Interface จะต้องสามารถถูกใช้งานแทนที่ Interface นั้นได้โดยไม่ทำให้การทำงานของระบบผิดเพี้ยน

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `service/impl/UserServiceImpl.java` | 24–82 | Implement ครบทุก method ของ `UserService` ตามสัญญา ไม่มีการ throw `UnsupportedOperationException` และคืนค่าที่ถูกต้องตรงตาม signature |
| `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder` | - | Implement `PasswordEncoder` อย่างถูกต้อง เมื่อถูกเรียกผ่าน `passwordEncoder.encode(...)` ใน `UserServiceImpl` (บรรทัด 40) ทำงานได้ตามสัญญาของ interface ทุกประการ |

---

## I: Interface Segregation Principle (ISP)

ไม่ควรบังคับให้ Client ต้องขึ้นต่อ Interface ที่มี method ที่ Client ไม่ได้ใช้งาน

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `service/UserService.java` | 8–15 | มีเฉพาะ Method ที่จำเป็นต่อการจัดการผู้ใช้ (`getUserById`, `createUser`, `updateUser`, `getAllUsers`, `exitsById`, `deleteUserById`) ไม่นำ method ของ Booking หรือ Equipment มาปนเปื้อน |
| `repository/UserRepository.java` | 9–11 | ขยายจาก `JpaRepository<User, Long>` และเพิ่มเฉพาะ Query method ที่จำเป็นคือ `findByUsername` ไม่ประกาศ method ที่ไม่ได้ใช้ |

---

## D: Dependency Inversion Principle (DIP)

โมดูลระดับสูง (High-level modules) ไม่ควรขึ้นต่อโมดูลระดับต่ำ (Low-level modules) แต่ทั้งคู่ควรขึ้นต่อ Abstraction

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `controller/api/UserController.java` | 26–31 | ขึ้นกับ Interface `UserService` (Abstraction) ไม่ได้ขึ้นกับ `UserServiceImpl` โดยตรง และรับ instance เข้ามาทาง Constructor Injection |
| `service/impl/UserServiceImpl.java` | 26–29 | ขึ้นกับ Interface `UserRepository` และ Interface `PasswordEncoder` ผ่าน Constructor Injection และฟิลด์ทั้งหมดเป็น `final` |
| `controller/web/AuthViewController.java` | 32–40 | รับ `UserRepository` และ `UserService` ผ่าน Constructor Injection |

แผนผัง Dependency Inversion ของส่วนงานนี้:

```
UserController ──────> UserService (Interface)
                            ▲
                            │ implements
                     UserServiceImpl ──────> UserRepository (Interface)
                                     ──────> PasswordEncoder (Interface)
                                     ──────> UserMapper
```

---

## Layered Architecture & Separation of Concerns

ส่วนงาน User แบ่งแยกความรับผิดชอบตาม Layer อย่างเคร่งครัด:

```
[Presentation Layer]    UserController (REST API) / AuthViewController (Web)
                               │ (ส่งเฉพาะ DTO: UserCreateRequest, UserUpdateRequest)
                               ▼
[Service Layer]         UserService / UserServiceImpl
                               │ (แปลง DTO ↔ Entity ผ่าน UserMapper)
                               ▼
[Data Access Layer]     UserRepository (Spring Data JPA)
                               │
                               ▼
[Domain / DB Layer]     User, UserProfile Entity ──> PostgreSQL Database
```

- **กฎ Layer:** Controller ไม่มีการเรียก Database โดยตรงผ่าน SQL, Service จัดการ logic ทั้งหมด และ Entity จะไม่ถูกส่งออกทาง REST API โดยตรงเพื่อป้องกันข้อมูลรั่วไหล
