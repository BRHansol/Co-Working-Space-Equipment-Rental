# ส่วนงานคนที่ 5: Observer, Error Handling, Swagger และ Deployment

| | |
|---|---|
| ชื่อ | นายจีรภัทร แก้วดี |
| รหัสนักศึกษา | 673380577-9 |
| Branch | `jiraphat_673380577-9_03` |
| รับผิดชอบ | ประวัติการเปลี่ยนสถานะการจอง (BookingStatusHistory), Observer Pattern แจ้งเตือนเมื่อสถานะเปลี่ยน, Global Exception Handler, Swagger, Docker, CI/CD และ Deploy ขึ้น Cloud |

## เอกสารในโฟลเดอร์นี้

| ไฟล์ | เนื้อหา |
|---|---|
| [solid-analysis.md](solid-analysis.md) | SOLID 5 ข้อของส่วนนี้ พร้อมไฟล์และบรรทัด + Layered Architecture |
| [design-patterns.md](design-patterns.md) | Observer Pattern: ปัญหาที่แก้, บทบาทแต่ละคลาส, Class Diagram, Sequence Diagram, ข้อจำกัด |
| [error-handling.md](error-handling.md) | Global Exception Handler, รูปแบบ ErrorResponse, ตาราง exception → status, Swagger |
| [HOWTORUN.md](HOWTORUN.md) | วิธีรัน test, รันบนเครื่อง, Docker Compose, deploy บน Render, CI/CD และแก้ปัญหาที่พบบ่อย |
| [diagrams/component-deployment-diagram.md](diagrams/component-deployment-diagram.md) | Component Diagram, Deployment Diagram (Cloud และ Docker Compose), ตารางการเชื่อมต่อเครือข่าย, สเปกเครื่อง, Environment Variables |
| `diagrams/png/` | รูป PNG ไว้ใส่สไลด์: `component-diagram`, `deployment-cloud-diagram`, `deployment-local-diagram`, `class-observer`, `sequence-update-status` |

## โค้ดที่รับผิดชอบ

```
code/
├── Dockerfile                      (multi-stage build, รันด้วย user ที่ไม่ใช่ root)
├── docker-compose.yml              (รันแอปใน container เชื่อม PostgreSQL ภายนอก)
├── .dockerignore
└── src/main/java/com/example/roombooking/
    ├── domain/entity/BookingStatusHistory.java
    ├── repository/BookingStatusHistoryRepository.java
    ├── event/BookingStatusChangedEvent.java
    ├── event/NotificationListener.java
    ├── service/NotificationService.java
    ├── service/impl/NotificationServiceImpl.java
    ├── service/BookingStatusHistoryService.java
    ├── service/impl/BookingStatusHistoryServiceImpl.java
    ├── controller/api/BookingStatusHistoryController.java   (GET /api/v1/bookings/{id}/status-history)
    ├── dto/response/BookingStatusHistoryResponse.java
    ├── config/JpaAuditingConfig.java
    ├── config/SwaggerConfig.java
    ├── exception/GlobalExceptionHandler.java
    └── dto/response/ErrorResponse.java
.github/workflows/ci-cd.yml         (Build → Test → Deploy)
```

## Test

เทสต์อยู่ที่ `test/jiraphat_673380577-9_03/java/` (รวม 43 test cases) และถูกรันพร้อม test ของทุกคนผ่าน `build-helper-maven-plugin` ใน `pom.xml`

| ไฟล์ | ทดสอบอะไร |
|---|---|
| `repository/BookingStatusHistoryRepositoryTest` | `@DataJpaTest`: `changedAt` ถูกเติมอัตโนมัติจาก JPA Auditing, ความสัมพันธ์กับ Booking/User ถูกบันทึก, สถานะเก็บเป็นข้อความไม่ใช่ตัวเลข, ค้นประวัติของ booking เดียวเรียงจากล่าสุด |
| `service/impl/NotificationServiceImplTest` | Mockito: สร้าง `BookingStatusHistory` ที่ถูกต้องแล้ว save |
| `event/NotificationListenerTest` | listener ส่งข้อมูลจาก event ต่อให้ `NotificationService` ครบ รวมกรณีระบบเปลี่ยนสถานะเอง (ไม่มีผู้เปลี่ยน) |
| `event/BookingStatusObserverIntegrationTest` | `@SpringBootTest`: อนุมัติแล้วมี event และประวัติ, ทุกการเปลี่ยนสถานะเพิ่มประวัติ 1 แถว, เปลี่ยนสถานะผิดกฎแล้วไม่มี event และไม่มีประวัติ |
| `exception/GlobalExceptionHandlerTest` | `@WebMvcTest`: แต่ละ exception ได้ status code ถูกต้อง (400/403/404/405/409/500) และรูปแบบ `ErrorResponse` ครบ, ไม่หลุดข้อความ SQL หรือรายละเอียดภายในออกไป, controller หน้าเว็บไม่ถูกจับเป็น JSON |
| `exception/InvalidSortIntegrationTest` | `?sort=string` (ค่าตัวอย่างใน Swagger) ที่ rooms, equipments, rooms/{id}/bookings ต้องได้ 400 ไม่ใช่ 500 และ sort ด้วย field ที่มีจริงยังใช้ได้ |
| `service/impl/BookingStatusHistoryServiceImplTest` | Mockito: แปลง history เป็น DTO ครบทุก field, ไม่มีผู้เปลี่ยนแสดงเป็น `system`, booking ที่ไม่มีจริงได้ 404 |
| `controller/api/BookingStatusHistoryControllerIntegrationTest` | `@SpringBootTest`: เปลี่ยนสถานะจริงแล้วเรียก API เห็นประวัติเรียงจากล่าสุด, ไม่มีการเปลี่ยนได้ `[]`, id ไม่มีจริงได้ 404, id เป็นตัวอักษรได้ 400 |
| `config/SwaggerConfigTest` | `/v3/api-docs` มีข้อมูล API และ endpoint ของ booking, ไม่ประกาศระบบ login ที่ยังไม่มี, Swagger UI เปิดได้ |

### วิธีรัน

```bash
cd code
./mvnw clean test          # Windows: .\mvnw.cmd clean test
```

test ใช้ H2 in-memory (`code/src/test/resources/application.properties`) ไม่ต้องเปิด PostgreSQL

---

## Deployment

### URL ระบบจริง (Render)

| | URL |
|---|---|
| REST API | https://room-booking-api-k47v.onrender.com/api/v1/rooms |
| Swagger UI | https://room-booking-api-k47v.onrender.com/swagger-ui.html |
| หน้าเว็บ | https://web-service-m1fz.onrender.com |

Render แบบฟรีจะหยุด service เมื่อไม่มีคนใช้ประมาณ 15 นาที request แรกหลังจากนั้นจะช้าประมาณ 30–60 วินาที

แอปเดียวกันถูก deploy เป็น 2 service ใช้ฐานข้อมูล PostgreSQL ตัวเดียวกัน

| Service | `SPRING_PROFILES_ACTIVE` | เปิดอะไร |
|---|---|---|
| `room-booking-api` | `api` | REST API + Swagger |
| `web-service` | `prod` | หน้าเว็บ, login/session, สิทธิ์ USER/STAFF/ADMIN, CSRF (ปิด `/api/**` ด้วย 403) |

ที่ต้องแยกเพราะ REST API ระบุผู้ใช้ด้วย header `X-User-Id` ซึ่ง client ส่งเองได้ ถ้าเปิดไว้บน service เดียวกับหน้าเว็บ จะใช้ API ข้ามการตรวจสิทธิ์ของหน้าเว็บได้

ขั้นตอนทั้งหมดอยู่ใน [HOWTORUN.md](HOWTORUN.md):
- รันบนเครื่อง (ข้อ 3)
- เตรียมบัญชี ADMIN และข้อมูลเริ่มต้น (ข้อ 4)
- Docker Compose (ข้อ 5)
- Environment Variables บน Render (ข้อ 6.3)

### CI/CD (GitHub Actions)

ไฟล์ `.github/workflows/ci-cd.yml` ทำงานเมื่อ push หรือเปิด pull request เข้า `main` / `develop`

```
Build (mvn package) → Test (mvn test + Test Report) → Deploy to Render
```

| Job | ทำอะไร |
|---|---|
| **Build** | คอมไพล์และสร้างไฟล์ jar แล้วเก็บเป็น artifact `app-jar` |
| **Test** | รัน test ทั้งหมดของทุกคน (H2) แล้วสร้าง Test Report เก็บเป็น artifact `test-report` |
| **Deploy to Render** | รันเฉพาะตอน push (หลัง merge) และเมื่อ test ผ่านแล้วเท่านั้น โดยสั่ง deploy ทั้ง 2 service ผ่าน Deploy Hook ใน GitHub Secrets (`RENDER_DEPLOY_HOOK_URL`, `RENDER_WEB_DEPLOY_HOOK_URL`) |

ใน pull request job Deploy จะขึ้น Skipped ซึ่งถูกต้อง เพราะยังไม่ deploy โค้ดที่ยังไม่ได้ merge

### Test Report

- **บน GitHub:** แท็บ **Actions** → เลือก run → ส่วน **Artifacts** ด้านล่าง → ดาวน์โหลด `test-report` แล้วเปิด `reports/surefire.html`
- **บนเครื่อง:**
  ```bash
  cd code
  ./mvnw test surefire-report:report-only
  ```
  แล้วเปิด `code/target/reports/surefire.html`
