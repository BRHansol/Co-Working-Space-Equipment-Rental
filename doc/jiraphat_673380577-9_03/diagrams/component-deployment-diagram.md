# Component Diagram & Deployment Diagram (ส่วนของคนที่ 5)
## โครงการ: Co-Working Space Equipment Rental (ระบบจองห้องประชุมและอุปกรณ์)
**ผู้รับผิดชอบ**: คนที่ 5 — Notification (Observer Pattern) + Error Handling + Infra/Deployment  
**มาตรฐานแผนภาพ**: UML 2.5 Component & Deployment Architecture Specifications  
**เทคโนโลยีหลัก**: Spring Boot 4.1.1 (Spring Framework 7.0), Java 17, PostgreSQL 15, Docker, GitHub Actions, Render

ขอบเขตงานของคนที่ 5 ในแผนภาพนี้:

| ส่วนประกอบ | ไฟล์ |
| :--- | :--- |
| Entity ประวัติการเปลี่ยนสถานะ | `domain/entity/BookingStatusHistory.java`, `repository/BookingStatusHistoryRepository.java` |
| Observer Pattern | `event/BookingStatusChangedEvent.java`, `event/NotificationListener.java`, `service/NotificationService.java`, `service/impl/NotificationServiceImpl.java` |
| JPA Auditing (เติม `changedAt` อัตโนมัติ) | `config/JpaAuditingConfig.java` |
| Error Handling | `exception/GlobalExceptionHandler.java`, `dto/response/ErrorResponse.java` |
| API Documentation | `config/SwaggerConfig.java` (springdoc-openapi 3.1.0) |
| Infra / Deployment | `Dockerfile`, `.dockerignore`, `docker-compose.yml`, `server.port=${PORT:8080}`, Render 2 service (API + หน้าเว็บ) |
| CI/CD | `.github/workflows/ci-cd.yml` (Build → Test → Deploy ผ่าน Render Deploy Hook) |

---

## 1. Component Diagram (แผนภาพแสดงองค์ประกอบและสถาปัตยกรรมภายใน)

Component Diagram แสดงส่วนประกอบซอฟต์แวร์ (Software Components), อินเทอร์เฟซที่ให้บริการ (Provided Interfaces), อินเทอร์เฟซที่เรียกใช้ (Required Interfaces) และการพึ่งพาระหว่างเลเยอร์ของ Spring Boot โดย**กล่องสีส้มคือส่วนที่คนที่ 5 รับผิดชอบ** กล่องสีเทาคือส่วนของสมาชิกคนอื่นที่คนที่ 5 ต้องเชื่อมต่อด้วย

```mermaid
flowchart TB
    %% ==========================================
    %% CLIENT TIER
    %% ==========================================
    subgraph ClientTier[" Client Tier "]
        RestClient["REST Client<br/>(Frontend / Postman / curl)<br/>ส่ง header X-User-Id"]
        SwaggerUI["Swagger UI ใน Web Browser<br/>/swagger-ui/index.html"]
    end

    %% ==========================================
    %% APPLICATION TIER (SPRING BOOT)
    %% ==========================================
    subgraph AppTier[" Application Tier (Spring Boot 4.1.1 Container) "]

        %% Presentation Layer
        subgraph WebLayer[" 1. Presentation Layer Components "]
            ApiControllers["BookingController / RoomController<br/>EquipmentController / UserController<br/>(@RestController ใน controller.api)"]
            ViewController["หน้าเว็บ Thymeleaf (คนที่ 4)<br/>BookingViewController, AdminViewController ฯลฯ<br/>(@Controller ใน controller.web)"]
            ExAdvice["GlobalExceptionHandler<br/>(@RestControllerAdvice<br/>basePackages = controller.api)"]
            ErrorDTO["ErrorResponse DTO<br/>timestamp, status, error,<br/>message, path, details"]
            OpenAPI["SpringDoc OpenAPI 3.1.0<br/>+ SwaggerConfig (@Configuration)<br/>/v3/api-docs"]
        end

        %% Domain Exceptions
        subgraph ExLayer[" Domain Exceptions (RuntimeException) "]
            Exceptions["ResourceNotFoundException → 404<br/>ForbiddenException → 403<br/>RoomNotAvailableException → 409<br/>EquipmentNotAvailableException → 409<br/>InvalidStateTransitionException → 409<br/>ConflictException → 409<br/>(Spring Data) PropertyReferenceException<br/>เช่น ?sort=string → 400"]
        end

        %% Service Layer
        subgraph SvcLayer[" 2. Service Layer Components (Business Logic & Patterns) "]
            BookingSvc["BookingService & BookingServiceImpl<br/>updateStatus() (@Transactional)"]
            StateCtx["State Pattern: BookingContext<br/>(คนที่ 3)"]

            subgraph ObserverPatternComp[" Observer Pattern (Booking Status Notification) "]
                StatusEvent["BookingStatusChangedEvent<br/>(extends ApplicationEvent)<br/>booking, oldStatus, newStatus, changedBy"]
                SpringEventBus["ApplicationEventPublisher<br/>(Spring Core Event Bus)"]
                Listener["NotificationListener<br/>(@Async + @TransactionalEventListener<br/>phase = AFTER_COMMIT)"]
                NotifySvc["NotificationService & NotificationServiceImpl<br/>บันทึกประวัติ + แจ้งเตือน (log)"]
            end
        end

        %% Persistence Layer
        subgraph DataLayer[" 3. Persistence Layer Components (Spring Data JPA) "]
            HistoryRepo["BookingStatusHistoryRepository<br/>findByBooking_IdOrderByChangedAtDesc()"]
            HistoryEntity["BookingStatusHistory (@Entity)<br/>ตาราง booking_status_history<br/>ManyToOne → Booking, User"]
            Auditing["JpaAuditingConfig<br/>(@EnableJpaAuditing)<br/>เติม changedAt ผ่าน @CreatedDate"]
            BookingRepo["BookingRepository<br/>(คนที่ 3)"]
            HibernateORM["Hibernate ORM 7.4"]
            HikariCP["HikariCP 7.0 Connection Pool"]
        end
    end

    %% ==========================================
    %% DATABASE TIER
    %% ==========================================
    subgraph DatabaseTier[" Database Tier "]
        RDBMS[("PostgreSQL 15 (Production / Docker)<br/>H2 2.4 In-Memory (ตอนรัน Test)")]
    end

    %% ==========================================
    %% CONNECTIONS & INTERFACES
    %% ==========================================
    %% Client to Presentation
    RestClient -->|"HTTP JSON / REST API /api/v1/**"| ApiControllers
    SwaggerUI -->|"HTTP GET /v3/api-docs"| OpenAPI
    OpenAPI -.->|"สแกน endpoint"| ApiControllers

    %% Presentation to Service
    ApiControllers --> BookingSvc

    %% Error handling flow
    BookingSvc -.->|"throw"| Exceptions
    Exceptions -.->|"หลุดออกจาก controller.api"| ExAdvice
    ExAdvice -->|"สร้าง"| ErrorDTO
    ErrorDTO -->|"HTTP 400/403/404/409/500 JSON"| RestClient
    ViewController -.-x|"ไม่อยู่ใน basePackages<br/>ใช้ WebHttpErrorAdvice ของหน้าเว็บ"| ExAdvice

    %% Observer flow
    BookingSvc -->|"1. เปลี่ยนสถานะ"| StateCtx
    BookingSvc -->|"2. save"| BookingRepo
    BookingSvc -->|"3. publishEvent()"| SpringEventBus
    SpringEventBus -->|"ส่ง"| StatusEvent
    StatusEvent -->|"4. หลัง commit (thread แยก)"| Listener
    Listener -->|"notifyBookingStatusChanged()"| NotifySvc
    NotifySvc -->|"5. save history"| HistoryRepo

    %% Persistence to Database
    HistoryRepo --> HistoryEntity
    Auditing -.->|"@CreatedDate"| HistoryEntity
    HistoryEntity --> HibernateORM
    BookingRepo --> HibernateORM
    HibernateORM --> HikariCP
    HikariCP -->|"JDBC / TCP 5432"| RDBMS

    %% ==========================================
    %% STYLES
    %% ==========================================
    classDef p5 fill:#ffe0b2,stroke:#e65100,stroke-width:2px,color:#000
    classDef other fill:#eeeeee,stroke:#9e9e9e,color:#333
    class ExAdvice,ErrorDTO,OpenAPI,StatusEvent,Listener,NotifySvc,HistoryRepo,HistoryEntity,Auditing p5
    class ApiControllers,ViewController,BookingSvc,StateCtx,BookingRepo,Exceptions other
```

### ลำดับการทำงานของ Observer Pattern (ตามหมายเลขในแผนภาพ)

1. `BookingServiceImpl.updateStatus()` ให้ `BookingContext` (State Pattern) ตรวจว่าเปลี่ยนสถานะได้ไหม ถ้าไม่ได้จะโยน `InvalidStateTransitionException` และ**ไม่มี event ถูกส่ง**
2. บันทึก `Booking` ที่เปลี่ยนสถานะแล้วผ่าน `BookingRepository`
3. ส่ง `BookingStatusChangedEvent` ผ่าน `ApplicationEventPublisher` โดย `BookingServiceImpl` ไม่รู้จัก listener ตัวไหนเลย (loose coupling)
4. `NotificationListener` ทำงาน**หลัง transaction commit แล้วเท่านั้น** (`AFTER_COMMIT`) บน thread แยก (`@Async`) ถ้า transaction rollback จะไม่มีการแจ้งเตือนหรือประวัติปลอมหลุดออกไป
5. `NotificationServiceImpl` บันทึก `BookingStatusHistory` ลงฐานข้อมูล โดย `changedAt` ถูกเติมอัตโนมัติจาก JPA Auditing แล้ว log การแจ้งเตือน (จุดต่อยอดสำหรับอีเมล/SMS ในอนาคตโดยไม่ต้องแก้ `BookingServiceImpl`)

---


## 2. Deployment Diagram (แผนภาพการติดตั้งระบบขึ้นใช้งานจริง)

ระบบรันได้ 3 สภาพแวดล้อม ทุกแบบใช้ `Dockerfile` ตัวเดียวกัน ต่างกันแค่ **Spring profile** และตัวแปรสภาพแวดล้อม

| สภาพแวดล้อม | ใช้ทำอะไร | ฐานข้อมูล |
| :--- | :--- | :--- |
| **Cloud (Render)** | ระบบจริงที่มี URL สาธารณะ | Render PostgreSQL (managed) |
| **บนเครื่อง / Docker Compose** | รันทดสอบบนเครื่องตัวเอง | PostgreSQL ใน Docker (`postgres:15-alpine`) |
| **Test (`mvn test`)** | รัน test อัตโนมัติบนเครื่องและใน GitHub Actions | H2 in-memory |

| Profile | เปิดอะไร |
| :--- | :--- |
| `prod` (ค่าเริ่มต้น = `web` + `postgres`) | หน้าเว็บ Thymeleaf, session, สิทธิ์ USER/STAFF/ADMIN, CSRF โดยปิด `/api/**` (403) และ Swagger |
| `api` (= `postgres`) | REST API `/api/v1/**` + Swagger UI ไม่มีหน้าเว็บ |

ระบบ deploy เป็น **2 service** ที่ใช้ฐานข้อมูลเดียวกัน เพราะ REST API ระบุผู้ใช้ด้วย header `X-User-Id` ซึ่ง client ส่งเองได้ ถ้าเปิด API บน service เดียวกับหน้าเว็บ จะใช้ API ข้ามการตรวจสิทธิ์ของหน้าเว็บได้

ทั้งสองแบบใช้ `ddl-auto=validate` คือแอปตรวจว่าตารางตรงกับ Entity เท่านั้น ส่วนการสร้างตารางต้องทำแยกก่อน ด้วย `db/postgresql/schema.sql`

### 2.1 Cloud Deployment (Render + GitHub Actions)

```mermaid
flowchart LR
    subgraph Dev[" Developer "]
        Git["git push / merge PR<br/>เข้า develop"]
    end

    subgraph GH[" GitHub "]
        Repo["Repository<br/>BRHansol/Co-Working-Space-Equipment-Rental"]
        subgraph Actions[" GitHub Actions: .github/workflows/ci-cd.yml "]
            Build["Job: Build<br/>mvn package"]
            Test["Job: Test<br/>mvn test (H2)<br/>+ Surefire Test Report"]
            Deploy["Job: Deploy to Render<br/>curl POST Deploy Hook × 2<br/>(เฉพาะ push ไม่รันใน PR)"]
        end
        Secrets[["Secrets<br/>RENDER_DEPLOY_HOOK_URL<br/>RENDER_WEB_DEPLOY_HOOK_URL"]]
    end

    subgraph Render[" Render Cloud (Singapore) "]
        LB["Render Edge<br/>HTTPS (TLS) : 443"]
        subgraph ApiSvc[" Web Service: room-booking-api "]
            ApiC["Docker container<br/>eclipse-temurin:17-jre-alpine<br/>SPRING_PROFILES_ACTIVE=api<br/>REST API + Swagger"]
        end
        subgraph WebSvc[" Web Service: web-service "]
            WebC["Docker container<br/>image เดียวกัน<br/>SPRING_PROFILES_ACTIVE=prod<br/>Thymeleaf + session + CSRF"]
        end
        PG[("Render PostgreSQL<br/>(managed)<br/>ใช้ร่วมกันทั้ง 2 service")]
    end

    subgraph Users[" Client "]
        Browser["Web Browser"]
        ApiClient["REST Client / Swagger UI"]
    end

    Git --> Repo
    Repo -->|"push / pull_request"| Build
    Build --> Test
    Test -->|"ผ่านทั้งหมด"| Deploy
    Secrets -.-> Deploy
    Deploy -->|"HTTPS POST"| ApiSvc
    Deploy -->|"HTTPS POST"| WebSvc
    Repo -.->|"Render ดึงโค้ด develop<br/>แล้ว docker build"| ApiSvc
    Repo -.-> WebSvc

    Browser -->|"HTTPS"| LB
    ApiClient -->|"HTTPS"| LB
    LB -->|"room-booking-api-k47v.onrender.com<br/>/api/v1/**, /swagger-ui.html"| ApiC
    LB -->|"web-service-m1fz.onrender.com<br/>/, /login, /rooms, /admin"| WebC
    ApiC -->|"JDBC / TCP 5432<br/>Internal URL"| PG
    WebC -->|"JDBC / TCP 5432<br/>Internal URL"| PG
```

### 2.2 Local Deployment (บนเครื่อง / Docker Compose)

```mermaid
flowchart LR
    subgraph UserTier[" Client Node (เครื่องนักพัฒนา) "]
        Browser["Web Browser"]
        ApiClient["REST Client / Swagger UI"]
    end

    subgraph Host[" เครื่องนักพัฒนา (Docker Desktop) "]
        subgraph BuildStage[" docker compose up --build: Build Stage "]
            Maven["maven:3.9-eclipse-temurin-17-alpine<br/>mvn dependency:go-offline (cache layer)<br/>mvn clean package -Dmaven.test.skip=true"]
        end
        subgraph AppContainer[" Container: app (docker-compose.yml) "]
            AppJar["app.jar, profile prod<br/>JRE 17 alpine, user 'spring' (non-root)<br/>port ${PORT:-8080}"]
        end
        ApiRun["mvnw spring-boot:run<br/>profile api, PORT=8081"]
        subgraph DbContainer[" Container: room-booking-db "]
            PostgreSQL["postgres:15-alpine<br/>room_booking : 5432<br/>ตารางจาก schema.sql"]
        end
    end

    subgraph TestEnv[" Test Environment (mvn test / GitHub Actions) "]
        TestJVM["JUnit 5 / Mockito / MockMvc<br/>@SpringBootTest, @DataJpaTest, @WebMvcTest"]
        H2[("H2 In-Memory<br/>MODE=PostgreSQL")]
    end

    Browser -->|"HTTP localhost:8080"| AppJar
    ApiClient -->|"HTTP localhost:8081<br/>/swagger-ui.html"| ApiRun
    Maven -->|"COPY --from=builder"| AppJar
    AppJar -->|"JDBC host.docker.internal:5432"| PostgreSQL
    ApiRun -->|"JDBC localhost:5432"| PostgreSQL
    TestJVM -->|"JDBC (in-process)"| H2
```

### คำสั่งที่ใช้ (รายละเอียดใน [HOWTORUN.md](../HOWTORUN.md))

| งาน | คำสั่ง |
| :--- | :--- |
| รัน test ทั้งหมด | `cd code && ./mvnw clean test` |
| สร้างตาราง | `psql ... -f code/src/main/resources/db/postgresql/schema.sql` |
| รันหน้าเว็บบนเครื่อง | `./mvnw spring-boot:run` (ตั้ง `DB_*` และ `SESSION_COOKIE_SECURE=false`) |
| รัน API + Swagger บนเครื่อง | `./mvnw spring-boot:run -Dspring-boot.run.profiles=api` |
| รันหน้าเว็บด้วย Docker Compose | `cd code && docker compose up --build` (ตั้ง `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) |

---

## 3. ตารางการเชื่อมต่อเครือข่าย (Network & Protocols Matrix)

| ต้นทาง | ปลายทาง | โปรโตคอล | พอร์ต | ความปลอดภัย | วัตถุประสงค์ |
| :--- | :--- | :---: | :---: | :--- | :--- |
| **Browser** | **Render Edge → web-service** | HTTPS | `443` | TLS จาก Render, session cookie แบบ Secure/HttpOnly/SameSite=Lax, CSRF token | ใช้หน้าเว็บ |
| **REST Client** | **Render Edge → room-booking-api** | HTTPS | `443` | TLS จาก Render, ระบุผู้ใช้ด้วย `X-User-Id` (ยังไม่มี JWT) | เรียก REST API / Swagger |
| **Render Edge** | **container ทั้งสอง** | HTTP | `$PORT` | อยู่ในเครือข่ายของ Render, แอปอ่าน `X-Forwarded-*` (`forward-headers-strategy=framework`) | ส่ง request เข้า container |
| **container ทั้งสอง** | **Render PostgreSQL** | JDBC over TCP | `5432` | Internal URL (private network), รหัสผ่านอยู่ใน Environment ไม่อยู่ในโค้ด | อ่าน/เขียนข้อมูลและ `booking_status_history` |
| **GitHub Actions** | **Render Deploy Hook** | HTTPS POST | `443` | URL เก็บใน GitHub Secrets | สั่ง deploy หลัง test ผ่าน |
| **app container (local)** | **room-booking-db** | JDBC over TCP | `5432` | ผ่าน `host.docker.internal` | ฐานข้อมูลบนเครื่อง |
| **JVM ตอนรัน test** | **H2 In-Memory** | JDBC (in-process) | - | ไม่มีการเชื่อมต่อเครือข่าย | รัน test โดยไม่พึ่งฐานข้อมูลจริง |

---

## 4. ข้อกำหนดทรัพยากรระบบ (System Specifications)

| ส่วนประกอบ | ที่ใช้จริง (Render Free) | แนะนำสำหรับ Production | เทคโนโลยี |
| :--- | :--- | :--- | :--- |
| **Application (ต่อ 1 service)** | 0.1 CPU, 512 MB RAM | 1 vCPU, 1 GB RAM | Docker, Alpine, Temurin JRE 17, Spring Boot 4.1.1, Tomcat 11 |
| **Database** | Render PostgreSQL Free | 1 vCPU, 1 GB RAM, 10 GB SSD + Backup | Managed PostgreSQL |
| **CI/CD** | GitHub Actions `ubuntu-latest` | เหมือนเดิม | JDK 17 (Temurin) + Maven |
| **Build** | Docker multi-stage build บน Render | เหมือนเดิม | `maven:3.9-eclipse-temurin-17-alpine` → `eclipse-temurin:17-jre-alpine` |

Render Free จะหยุด service เมื่อไม่มีคนใช้ประมาณ 15 นาที request แรกหลังจากนั้นจะช้าประมาณ 30–60 วินาที

### ตัวแปรสภาพแวดล้อม (Environment Variables) บน Render

| ตัวแปร | `room-booking-api` | `web-service` | ใช้ทำอะไร |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `api` | `prod` | เลือกโหมด API หรือหน้าเว็บ |
| `DB_URL` | `jdbc:postgresql://<internal-host>:5432/<db-name>` | เหมือนกัน | ที่อยู่ฐานข้อมูล (ขึ้นต้นด้วย `jdbc:` และไม่ใส่ user:password ใน URL) |
| `DB_USERNAME` / `DB_PASSWORD` | จากหน้า Render PostgreSQL | เหมือนกัน | บัญชีฐานข้อมูล |
| `JAVA_TOOL_OPTIONS` | `-Xmx300m -Xss512k -XX:MaxMetaspaceSize=150m -XX:+UseSerialGC -XX:TieredStopAtLevel=1` | เหมือนกัน | จำกัดหน่วยความจำ JVM ให้พอดีกับ RAM 512 MB |
| `PORT` | Render ส่งให้เอง | Render ส่งให้เอง | พอร์ตที่ Tomcat เปิดรับ (`server.port=${PORT:8080}`) |
