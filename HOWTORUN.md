# วิธีรันและ deploy Co-Working-Space-Equipment-Rental

เว็บใช้ PostgreSQL/Aiven และ profile `prod` เป็นค่าเริ่มต้น คู่มือนี้ครอบคลุมการเตรียมฐานข้อมูล การรันเว็บ production และ Railway โดย H2 กับข้อมูลทดลองอยู่เฉพาะชุดทดสอบ ไม่มีบัญชี demo ใน runtime ขั้นตอนต่อไปนี้เป็นคำแนะนำ ไม่ใช่หลักฐานว่าได้ deploy หรือเชื่อม Aiven สำเร็จแล้ว

## 1. เตรียมเครื่องและ service

- ใช้ JDK 17 และตั้ง `JAVA_HOME`/`PATH` ให้เรียก `java` กับ `javac` ได้
- เปิด Maven project จาก `code/pom.xml`; ใช้ Maven Wrapper ของโครงการ
- เตรียม Aiven for PostgreSQL และจด host, port, database, username จาก service overview; เก็บ password ใน environment/Variables ของ service
- ติดตั้ง PostgreSQL client เพื่อให้เรียก `psql` ได้ และเตรียมบัญชีที่มีสิทธิ์สร้าง schema
- เตรียม Railway service สำหรับเว็บ และใช้ HTTPS domain เมื่อทดสอบ login/session

คำสั่งทั้งหมดที่อ้าง `src/` หรือ `mvnw` ให้รันจาก `code/`:

```powershell
Set-Location -LiteralPath '.\code'
java -version
javac -version
.\mvnw.cmd --version
```

หากอยู่ใน `code/` แล้วไม่ต้องเปลี่ยน directory ซ้ำ CMD ใช้ `mvnw.cmd` ส่วน macOS/Linux ใช้ `sh ./mvnw`

## 2. ตั้งค่า Aiven และ SSL

| Environment | ความหมาย |
| --- | --- |
| `DB_URL` | JDBC URL เช่น `jdbc:postgresql://YOUR_AIVEN_HOST:YOUR_AIVEN_PORT/YOUR_DATABASE?sslmode=require` |
| `DB_USERNAME` | ผู้ใช้ฐานข้อมูลจาก Aiven |
| `DB_PASSWORD` | รหัสผ่านฐานข้อมูลผ่าน environment; ไม่เก็บใน source หรือคำสั่งที่แชร์ |
| `PORT` | Railway ส่งให้แอป; ค่าเริ่มต้นเมื่อไม่มีคือ 8080 |
| `SPRING_PROFILES_ACTIVE` | ตั้ง `prod` ได้ หรือปล่อยให้ใช้ default `prod` |

ใช้ host/port/database จริงจาก Aiven อย่าสมมติ port เป็น 5432 และอย่านำ URI `postgres://username:password@...` มาใส่ `DB_URL` ตรง ๆ ต้องใช้รูปแบบ `jdbc:postgresql://...` และแยก credentials เป็น `DB_USERNAME`/`DB_PASSWORD`

`sslmode=require` บังคับเข้ารหัส TLS แต่ไม่ตรวจ server certificate/hostname ตาม [ตัวอย่าง Java ของ Aiven](https://aiven.io/docs/products/postgresql/howto/connect-java) หากเลือกตรวจ certificate และ hostname ใช้:

```text
jdbc:postgresql://YOUR_AIVEN_HOST:YOUR_AIVEN_PORT/YOUR_DATABASE?sslmode=verify-full&sslrootcert=/run/secrets/aiven-ca.pem
```

ต้องดาวน์โหลด Aiven project CA และจัดให้ไฟล์อยู่ใน container ที่แอปอ่านได้จริง ตำแหน่งตัวอย่างไม่ถูกสร้างโดย Dockerfile ของโครงการ อย่าอ้าง path ของเครื่องพัฒนาเป็น path ใน Railway ดู [Aiven TLS/SSL](https://aiven.io/docs/platform/concepts/tls-ssl-certificates) และ [pgJDBC SSL](https://jdbc.postgresql.org/documentation/ssl/)

## 3. เตรียม schema ก่อนเริ่มเว็บ

สำหรับ **ฐานข้อมูลว่าง** ใช้ `src/main/resources/db/postgresql/schema.sql` ซึ่งสร้างทั้ง 7 ตารางและไม่มี seed accounts/rooms/equipment รันด้วย `psql` แยกก่อนเริ่มแอป ตัวอย่างจาก `code/`:

```powershell
psql "host=YOUR_AIVEN_HOST port=YOUR_AIVEN_PORT dbname=YOUR_DATABASE user=YOUR_DATABASE_USER sslmode=require" -W -v ON_ERROR_STOP=1 --single-transaction -f '.\src\main\resources\db\postgresql\schema.sql'
```

`-W` ให้กรอกรหัสผ่านผ่าน prompt โดยไม่ใส่ password ลง command history หากใช้ certificate validation ให้เปลี่ยน connection parameters เป็น `sslmode=verify-full sslrootcert=YOUR_CA_FILE_PATH` ตามไฟล์ CA ที่มีอยู่จริง

ตรวจว่ามีตาราง `users`, `user_profile`, `meeting_rooms`, `equipment`, `bookings`, `booking_equipment` และ `booking_status_history` แล้วจึงเริ่มเว็บ runtime ใช้ `ddl-auto=validate` และ `spring.sql.init.mode=never` จึงไม่สร้างหรือแก้ schema ระหว่าง startup

Full schema รันซ้ำได้โดยคงข้อมูลเดิม แต่ `IF NOT EXISTS` ไม่อัปเกรด columns/constraints ของตารางที่มีอยู่แล้ว หากใช้ฐานข้อมูลเก่าให้สำรองข้อมูล ตรวจ schema และเตรียม migration ที่ตรงกับการเปลี่ยน Entity โดยไม่อ้างว่าสคริปต์นี้ซ่อมข้อมูลเดิมเอง

`db/nuttachai_673380581-8_04/schema.sql` เป็น upgrade เฉพาะ `booking_equipment` และต้องมี parent tables ก่อน ไม่ใช้แทน full schema สำหรับฐานข้อมูลใหม่ ไม่ต้องรัน `data.sql` ของสมาชิกหรือ SQL test fixtures ในฐานข้อมูลจริง

## 4. รันเว็บด้วย profile prod

`prod` รวม `web,postgres` หน้าเว็บใช้ session authentication, role guards และ CSRF; `/api/**` ถูกปิดด้วย HTTP 403 ค่า DB ทั้งสามไม่มี localhost fallback เมื่อไม่กำหนด environment หรือ schema ไม่ตรง แอปจะเริ่มไม่ได้

หลังตั้ง DB environment และเตรียม schema แล้ว:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

```sh
sh ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

แอป bind `0.0.0.0` และอ่าน `PORT` ไม่ต้องแก้เป็น loopback หน้าเว็บต้องเข้าโดย HTTPS หรือ reverse proxy ที่ส่ง forwarded headers ถูกต้อง เพราะ session cookie ตั้ง `Secure`, `HttpOnly` และ `SameSite=Lax` การเปิด HTTP ตรง ๆ ไม่เหมาะกับทดสอบ login ของ configuration production นี้

Profile `local`/`local-postgres`, H2 แบบไฟล์และ demo seeder ไม่ใช่โหมด runtime อีกต่อไป เมื่อเริ่มฐานข้อมูลว่างจะยังไม่มีห้องหรืออุปกรณ์

## 5. สมัครบัญชีและเตรียม ADMIN คนแรก

สมัครผ่าน `/register` บน HTTPS URL ของเว็บ บัญชีใหม่มี role `USER` ไม่มี ADMIN อัตโนมัติ ผู้ดูแลที่เข้าถึง DB console ได้สามารถตรวจและเลื่อน role ของบัญชีที่ตนสมัครไว้

เริ่มจากตรวจ username เป้าหมายให้ได้ **หนึ่งบัญชีที่ถูกต้อง** แทน `YOUR_REGISTERED_USERNAME` ด้วย username ที่สมัครจริง:

```sql
SELECT id, username, role, active
FROM users
WHERE LOWER(username) = LOWER('YOUR_REGISTERED_USERNAME');
```

หากได้หนึ่งบัญชีและยังเป็น USER ที่ active จึงรันใน transaction:

```sql
BEGIN;
UPDATE users
SET role = 'ADMIN'
WHERE username = 'YOUR_REGISTERED_USERNAME'
  AND role = 'USER'
  AND active = TRUE
  AND (SELECT COUNT(*) FROM users
       WHERE LOWER(username) = LOWER('YOUR_REGISTERED_USERNAME')) = 1
RETURNING id, username, role;
```

ตรวจผลที่คืนให้เป็นบัญชีเป้าหมายหนึ่งแถว แล้วรัน `COMMIT;` หากไม่ตรงหรือไม่คืนแถว ให้ `ROLLBACK;` แทน ไม่ใช้เลข ID สมมติหรือยกระดับทุกบัญชี จากนั้นเข้าสู่ระบบใหม่และใช้ `/admin` เพิ่มห้อง อุปกรณ์ หรือจัดการผู้ใช้ตามสิทธิ์

## 6. Deploy บน Railway

1. สร้างหรือเลือก service ของ repository/version ที่ทีมต้องการ deploy และตั้ง **Root Directory = `code`**
2. ใช้ **Dockerfile build** จาก `code/Dockerfile`; entrypoint รัน `java -jar app.jar`
3. ตั้ง Variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` ให้ชี้ Aiven ที่เตรียม schema แล้ว ตั้ง `SPRING_PROFILES_ACTIVE=prod` ได้เพื่อให้ชัดเจน
4. ใช้ `PORT` ที่ Railway จัดให้; แอปอ่านค่านี้และ bind `0.0.0.0`
5. Deploy แล้วตรวจ startup logs ว่า schema validation ผ่าน โดยไม่เผย DB password หรือ URL ที่ฝัง credentials
6. ที่ Settings → Networking → Public Networking เลือก **Generate Domain** แล้วเปิด HTTPS URL ที่ได้
7. ตรวจสมัคร/เข้าสู่ระบบ, สิทธิ์ USER/STAFF/ADMIN, CSRF และการจองที่บันทึกลง DB ก่อนบันทึก public URL ใน README

Railway ต้องใช้โค้ดเวอร์ชันที่มี configuration นี้จริง การแก้ working tree บนเครื่องไม่ได้ส่งขึ้น repository หรือ service เอง คู่มือนี้ไม่มีคำสั่ง commit/push หรือ deploy ที่ทำงานอัตโนมัติ

ดู [Railway Spring Boot](https://docs.railway.com/guides/spring-boot), [host/PORT](https://docs.railway.com/networking/troubleshooting/application-failed-to-respond), [Variables](https://docs.railway.com/variables) และ [Public Networking](https://docs.railway.com/guides/public-networking)

GitHub Actions ของทีมยังมี Render Deploy Hooks อยู่ การตั้ง Railway service ไม่ได้เปลี่ยน workflow เป็น Railway deployment อัตโนมัติ

## 7. Docker Compose กับ Aiven

Compose มี app service ที่เชื่อม PostgreSQL ภายนอก ต้องตั้ง `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` ใน environment ของ Terminal ก่อน ไม่สร้าง database service หรือ schema ให้เอง อย่าเขียน credentials จริงลง Compose/เอกสาร

รันจาก `code/` หลัง bootstrap schema:

```powershell
docker compose up --build -d
docker compose logs --tail=100 app
```

ให้ใช้ HTTPS reverse proxy สำหรับทดสอบเว็บ เพราะ cookie เป็น `Secure` และตั้ง port ของ proxy ให้ตรงกับ `PORT`/port mapping ของ Compose เมื่อต้องการหยุด containers:

```powershell
docker compose down
```

คำสั่งนี้หยุด app container แต่ไม่ลบฐานข้อมูล Aiven

## 8. โหมด API และชุดทดสอบ

Profile `api` รวม `postgres` ใช้ environment/schema ชุดเดียวกัน และเป็นโหมด backend สำหรับ API integration/testing:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=api"
```

API ยังใช้ contract เดิมจาก develop: booking ใช้ `X-User-Id` และยังไม่มี authentication ที่ตรวจตัวตนสมบูรณ์ จึงไม่ถือว่าเป็น public production API เว็บ `prod` ไม่เปิด API นี้ ส่วน Swagger/OpenAPI ของโหมด API อยู่ที่ `/swagger-ui.html` และ `/v3/api-docs`

รัน tests จาก `code/` โดยไม่ต้องใช้ Aiven:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -Pnuttachai_673380581-8_04 test
```

macOS/Linux ใช้ `sh ./mvnw test` Test configuration ใช้ H2 in-memory และ fixtures เฉพาะ test รายงานชุดรวมอยู่ที่ `target/surefire-reports/` ส่วน profile สมาชิกคนที่ 4 อยู่ที่ `target/nuttachai_673380581-8_04-surefire-reports/` จำนวนและผลผ่านให้อ่านรายงานล่าสุด การผ่าน H2 tests ไม่ยืนยัน Aiven/Railway runtime

## 9. แก้ปัญหาที่พบบ่อย

| อาการ | สิ่งที่ตรวจ |
| --- | --- |
| ไม่มี `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` | ตั้ง Variables ของ app service ให้ครบ; ไม่มี localhost fallback |
| `Schema-validation: missing table/column` | รัน full schema สำหรับ DB ว่าง หรือเตรียม migration สำหรับ DB เดิม; ไม่เปิด `ddl-auto=update` เพื่อข้ามการตรวจ |
| SSL/certificate error | ตรวจ `sslmode`, hostname และตำแหน่ง CA ใน container เมื่อใช้ `verify-full` |
| Railway ตอบ 502/Application failed to respond | ตรวจ `0.0.0.0`, `PORT` และ target port ของ domain ให้ตรงกัน |
| Login/session ไม่อยู่เมื่อเปิด HTTP | ใช้ HTTPS domain/reverse proxy; cookie production เป็น `Secure` |
| ส่งแบบฟอร์มแล้ว 403 | เข้าใช้งานผ่าน session ที่ถูกต้องและโหลดแบบฟอร์มใหม่เพื่อรับ CSRF token |
| `/api/**` ตอบ 403 บนเว็บ | เป็นการปิด API ของ profile `prod`; API integration ใช้ service/profile `api` แยก |
| ห้อง/อุปกรณ์ว่างทั้งหมด | Runtime ไม่ seed demo data; ผู้ดูแลเพิ่มข้อมูลจริงผ่าน `/admin` |
| ไม่พบ `mvnw.cmd`/`pom.xml` | กำหนด working directory เป็น `code/` และใช้ JDK 17 |
