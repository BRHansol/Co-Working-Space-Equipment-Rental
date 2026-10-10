# How to Run: Co-Working Space Equipment Rental

คู่มือรันระบบบนเครื่องตัวเอง รัน test และ deploy ขึ้น Render ตามที่ทีมใช้งานจริง

| | URL ระบบจริง (Render) |
|---|---|
| หน้าเว็บ | https://web-service-m1fz.onrender.com |
| REST API | https://room-booking-api-k47v.onrender.com/api/v1/rooms |
| Swagger UI | https://room-booking-api-k47v.onrender.com/swagger-ui.html |

Render แบบฟรีจะหยุด service เมื่อไม่มีคนใช้ประมาณ 15 นาที เปิดครั้งแรกหลังจากนั้นต้องรอ 30–60 วินาที

---

## 0. ระบบมี 2 โหมด (Spring profile)

แอปตัวเดียวกันเลือกโหมดด้วย profile ทั้งสองโหมดใช้ฐานข้อมูล PostgreSQL เดียวกันได้

| Profile | เปิดอะไร | ปิดอะไร |
|---|---|---|
| `prod` (ค่าเริ่มต้น = `web` + `postgres`) | หน้าเว็บ Thymeleaf, login/session, สิทธิ์ USER/STAFF/ADMIN, CSRF | `/api/**` (ตอบ 403) และ Swagger |
| `api` (= `postgres`) | REST API `/api/v1/**` และ Swagger UI | หน้าเว็บ |

แยกเป็น 2 โหมดเพราะ REST API ระบุผู้ใช้ด้วย header `X-User-Id` ซึ่ง client ส่งเองได้ ถ้าเปิดบน service เดียวกับหน้าเว็บ จะใช้ API ข้ามการตรวจสิทธิ์ของหน้าเว็บได้

---

## 1. สิ่งที่ต้องติดตั้ง

| โปรแกรม | ใช้ทำอะไร | ตรวจด้วย |
|---|---|---|
| JDK 17 ขึ้นไป | คอมไพล์และรันแอป (โค้ด compile เป็น Java 17) | `java -version` |
| Git | ดึงโค้ด | `git --version` |
| Docker Desktop | รัน PostgreSQL บนเครื่อง และ Docker Compose | `docker --version` |

ไม่ต้องติดตั้ง Maven เพราะโปรเจกต์มี Maven Wrapper (`mvnw.cmd` / `mvnw`) มาให้

```powershell
git clone https://github.com/BRHansol/Co-Working-Space-Equipment-Rental.git
cd Co-Working-Space-Equipment-Rental
git checkout develop
```

---

## 2. รัน test (ไม่ต้องมีฐานข้อมูล)

test ใช้ H2 in-memory จาก `code/src/test/resources/application.properties`

```powershell
cd code
.\mvnw.cmd clean test          # macOS/Linux: ./mvnw clean test
```

ต้องได้ `BUILD SUCCESS` ระบบจะรวม test จาก `code/src/test/java` และ `test/<branch>/java` ของสมาชิกทุกคนให้เอง

**ดู Test Report แบบ HTML**

```powershell
.\mvnw.cmd surefire-report:report-only
```

เปิดไฟล์ `code/target/reports/surefire.html`

---

## 3. รันระบบบนเครื่อง

### 3.1 เปิด PostgreSQL ด้วย Docker (ครั้งแรกครั้งเดียว)

```powershell
docker run -d --name room-booking-db `
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=room_booking `
  -p 5432:5432 postgres:15-alpine
```

ครั้งต่อไปใช้แค่ `docker start room-booking-db`

### 3.2 สร้างตาราง (ครั้งแรกครั้งเดียว)

แอปตั้ง `ddl-auto=validate` ไว้ คือตรวจว่าตารางตรงกับ Entity เท่านั้น ไม่สร้างตารางเอง จึงต้องรัน schema ก่อน (รันจาก root ของ repo)

```powershell
Get-Content code\src\main\resources\db\postgresql\schema.sql | docker exec -i room-booking-db psql -U postgres -d room_booking -v ON_ERROR_STOP=1
```

macOS/Linux:

```bash
docker exec -i room-booking-db psql -U postgres -d room_booking -v ON_ERROR_STOP=1 < code/src/main/resources/db/postgresql/schema.sql
```

ถ้าเห็น `CREATE TABLE` และ `CREATE INDEX` หลายบรรทัด แปลว่าเสร็จ รันซ้ำได้ ข้อมูลเดิมไม่หาย

### 3.3 ตั้งค่าการเชื่อมต่อฐานข้อมูล

PowerShell (ต้องตั้งใหม่ทุกครั้งที่เปิดหน้าต่างใหม่):

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/room_booking"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"
```

macOS/Linux: `export DB_URL=... DB_USERNAME=... DB_PASSWORD=...`

### 3.4 รันหน้าเว็บ (profile `prod`) ที่ http://localhost:8080

```powershell
cd code
$env:SESSION_COOKIE_SECURE = "false"
.\mvnw.cmd spring-boot:run
```

ต้องตั้ง `SESSION_COOKIE_SECURE=false` เฉพาะตอนรันบนเครื่อง เพราะ `http://localhost` ไม่ใช่ HTTPS ถ้าไม่ตั้ง login แล้วจะหลุดทันที บน Render ไม่ต้องตั้ง

### 3.5 รัน REST API + Swagger (profile `api`) ที่ http://localhost:8081

เปิด PowerShell หน้าต่างที่ 2 แล้วตั้ง `DB_*` แบบข้อ 3.3 ก่อน จากนั้นรัน:

```powershell
cd code
$env:PORT = "8081"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=api"
```

Swagger UI: http://localhost:8081/swagger-ui.html

ถ้าต้องการรันแค่โหมดเดียว ใช้ port 8080 ได้เลย ไม่ต้องตั้ง `PORT`

---

## 4. เตรียมบัญชีและข้อมูลเริ่มต้น

ระบบไม่สร้างบัญชี ห้อง หรืออุปกรณ์ตัวอย่างให้ ต้องเพิ่มเองตามขั้นตอนนี้

### 4.1 สร้างบัญชี ADMIN

**วิธีที่ 1: ผ่าน Swagger (ง่ายสุด)** ใช้ **POST `/api/v1/users`**

```json
{
  "username": "admin01",
  "email": "admin01@example.com",
  "password": "admin1234",
  "role": "ADMIN",
  "fullName": "ผู้ดูแลระบบ"
}
```

- `username` ต้องยาว 4–50 ตัวอักษร
- `password` ต้องยาวอย่างน้อย 6 ตัว
- `role` เลือกได้ `ADMIN`, `STAFF` หรือ `USER`

จากนั้น login ที่หน้าเว็บ `/login` ได้เลย

**วิธีที่ 2: สมัครที่หน้าเว็บ `/register` แล้วเลื่อน role ในฐานข้อมูล** (บัญชีที่สมัครจากหน้าเว็บจะได้ role `USER`)

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'ชื่อที่สมัคร';
```

- บนเครื่อง: `docker exec -it room-booking-db psql -U postgres -d room_booking`
- บน Render: หน้า PostgreSQL → **Connect** → **External** → คัดลอก PSQL Command หรือใช้ pgAdmin

เลื่อน role แล้วต้อง logout แล้ว login ใหม่

### 4.2 เพิ่มห้องและอุปกรณ์

login เป็น ADMIN แล้วเพิ่มที่หน้า `/admin` หรือเพิ่มผ่าน Swagger:

- **POST `/api/v1/rooms`**
  ```json
  { "name": "ห้องประชุม A", "capacity": 8, "floor": "2", "roomType": "STANDARD", "status": "AVAILABLE" }
  ```
- **POST `/api/v1/equipments`**
  ```json
  { "name": "Projector", "totalQuantity": 2, "category": "AV" }
  ```

ห้อง `STANDARD` อนุมัติการจองทันที ห้อง `VIP` ต้องรอ STAFF หรือ ADMIN อนุมัติ

### 4.3 ลำดับทดสอบที่แนะนำ

1. ADMIN เพิ่มห้อง STANDARD 1 ห้อง, VIP 1 ห้อง และอุปกรณ์
2. สมัคร USER แล้วจองห้อง STANDARD ต้องได้สถานะ **APPROVED** ทันที
3. USER จองห้อง VIP พร้อมอุปกรณ์ ต้องได้สถานะ **PENDING**
4. ADMIN อนุมัติการจองห้อง VIP
5. เช็กประวัติการเปลี่ยนสถานะ (Observer Pattern):
   ```sql
   SELECT booking_id, old_status, new_status, changed_by, changed_at
   FROM booking_status_history ORDER BY changed_at DESC;
   ```

---

## 5. Docker Compose

`code/docker-compose.yml` สร้าง image จาก `Dockerfile` แล้วรันหน้าเว็บ (profile `prod`) โดยเชื่อมกับ PostgreSQL ที่มีอยู่แล้ว (ไม่ได้สร้าง PostgreSQL ให้)

ตัวอย่างเชื่อมกับ PostgreSQL จากข้อ 3.1 บนเครื่อง Windows/macOS (Docker Desktop):

```powershell
cd code
$env:DB_URL = "jdbc:postgresql://host.docker.internal:5432/room_booking"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"
docker compose up --build
```

เปิด http://localhost:8080 และปิดด้วย `docker compose down`

- ใน container ต้องใช้ `host.docker.internal` แทน `localhost` เพราะ `localhost` ใน container หมายถึงตัว container เอง
- compose ไม่ได้ส่ง `SESSION_COOKIE_SECURE` เข้า container ถ้าเปิดผ่าน `http://localhost` หน้าเว็บจะเปิดได้ แต่ login แล้วจะหลุด ถ้าต้องการทดสอบ login บนเครื่อง ให้ใช้ข้อ 3.4 แทน

`Dockerfile` เป็น multi-stage build:
- **stage แรก:** `maven:3.9-eclipse-temurin-17-alpine` สร้างไฟล์ jar
- **stage สอง:** `eclipse-temurin:17-jre-alpine` รันแอปด้วย user ที่ไม่ใช่ root

---

## 6. Deploy บน Render

### 6.1 โครงสร้าง

| Render resource | ประเภท | Profile |
|---|---|---|
| PostgreSQL | Managed PostgreSQL | - |
| `room-booking-api` | Web Service (Docker) | `api` |
| `web-service` (หน้าเว็บ) | Web Service (Docker) | `prod` |

ทั้งสอง Web Service ใช้ repo เดียวกัน branch `develop`, Root Directory `code` และ build จาก `Dockerfile`

### 6.2 สร้างฐานข้อมูลและตาราง

1. Render → **New** → **Postgres** → เลือก region เดียวกับ service (Singapore)
2. คัดลอก **External Database URL** มาใช้รัน schema จากเครื่อง (ต้องติดตั้ง `psql`):
   ```bash
   psql "<External Database URL>" -v ON_ERROR_STOP=1 -f code/src/main/resources/db/postgresql/schema.sql
   ```
   ถ้าฐานข้อมูลเคยถูก Hibernate สร้างตารางไว้แล้ว (เวอร์ชันเก่าใช้ `ddl-auto=update`) ข้ามขั้นนี้ได้ ทีมทดสอบแล้วว่าตารางเดิมผ่าน `validate`

### 6.3 Environment Variables

| Key | `room-booking-api` | `web-service` |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `api` | `prod` |
| `DB_URL` | `jdbc:postgresql://<Internal Hostname>:5432/<Database>` | เหมือนกัน |
| `DB_USERNAME` | Username จากหน้า PostgreSQL | เหมือนกัน |
| `DB_PASSWORD` | Password จากหน้า PostgreSQL | เหมือนกัน |
| `JAVA_TOOL_OPTIONS` | `-Xmx300m -Xss512k -XX:MaxMetaspaceSize=150m -XX:+UseSerialGC -XX:TieredStopAtLevel=1` | เหมือนกัน |

- **`DB_URL`:** ใช้ Internal Hostname เพราะเร็วกว่าและไม่ต้องออกอินเทอร์เน็ต URL ต้องขึ้นต้นด้วย `jdbc:postgresql://` และห้ามใส่ `user:password@` ไว้ใน URL
- **ค่าที่ไม่ต้องตั้ง:** `PORT` (Render ส่งให้เอง), `SERVER_ADDRESS` (โค้ดตั้ง `0.0.0.0` ไว้แล้ว), `SPRING_JPA_HIBERNATE_DDL_AUTO` (ใช้ `validate` จากโค้ด)
- **`JAVA_TOOL_OPTIONS`:** จำกัดหน่วยความจำ JVM ให้พอดีกับ RAM 512 MB ของแพ็กเกจฟรี ถ้าไม่ตั้ง service อาจถูกปิดกลางทาง (exit 1 โดยไม่มี log)

### 6.4 CI/CD อัตโนมัติ (GitHub Actions)

`.github/workflows/ci-cd.yml` ทำงานเมื่อ push หรือเปิด PR เข้า `main` / `develop`

```
Build (mvn package) → Test (mvn test + Test Report) → Deploy to Render (เฉพาะ push)
```

ตั้งค่าครั้งเดียว:
1. Render แต่ละ service → **Settings** → **Deploy Hook** → คัดลอก URL
2. GitHub → **Settings** → **Secrets and variables** → **Actions** → สร้าง secret 2 ตัว
   - `RENDER_DEPLOY_HOOK_URL` สำหรับ `room-booking-api`
   - `RENDER_WEB_DEPLOY_HOOK_URL` สำหรับ `web-service`
3. Render ทั้งสอง service → **Settings** → **Auto-Deploy** = **Off** เพื่อให้ deploy หลัง test ผ่านเท่านั้น

Deploy Hook URL เป็นความลับ ห้ามใส่ในโค้ดหรือ README

ดูผล CI ที่แท็บ **Actions** ส่วน Test Report อยู่ใน **Artifacts** ของแต่ละ run (`test-report`)

---

## 7. แก้ปัญหาที่พบบ่อย

| อาการ | สาเหตุ / วิธีแก้ |
|---|---|
| `Could not resolve placeholder 'DB_URL'` | ยังไม่ได้ตั้ง `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (ข้อ 3.3) |
| `Schema-validation: missing table [...]` | ยังไม่ได้รัน `schema.sql` (ข้อ 3.2 / 6.2) |
| `password authentication failed` | รหัสผ่านฐานข้อมูลไม่ตรง |
| Login แล้วเด้งกลับหน้า login (บนเครื่อง) | ตั้ง `SESSION_COOKIE_SECURE=false` (ข้อ 3.4) |
| เรียก `/api/v1/...` แล้วได้ 403 | กำลังเรียกที่ service หน้าเว็บ (`prod`) ให้เรียกที่ service `api` |
| `/swagger-ui.html` ได้ 404 | service นั้นไม่ได้ใช้ profile `api` |
| หน้า "ค้นหาพื้นที่" ขึ้น "พบ 0 พื้นที่" | ยังไม่มีห้องในระบบ เพิ่มตามข้อ 4.2 |
| เข้า `/admin` ไม่ได้ (403) | บัญชีเป็น `USER` ให้เลื่อน role ตามข้อ 4.1 แล้ว login ใหม่ |
| Render: `'url' must start with "jdbc"` | `DB_URL` ต้องขึ้นต้นด้วย `jdbc:postgresql://` |
| Render: `invalid port number` | เอา `user:password@` ออกจาก `DB_URL` |
| Render: `database "..." does not exist` | ชื่อท้าย URL ต้องตรงกับช่อง **Database** ในหน้า PostgreSQL |
| Render: exit status 1 โดยไม่มี log | ตั้ง `JAVA_TOOL_OPTIONS` ตามข้อ 6.3 |
| `?sort=string` ใน Swagger ได้ 400 | ปกติ: `string` เป็นค่าตัวอย่าง ให้ลบช่อง sort ออก หรือใส่ชื่อ field จริง เช่น `name,asc` |
