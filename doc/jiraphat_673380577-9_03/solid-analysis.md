# SOLID Analysis: ส่วน Notification (Observer), Error Handling และ Swagger

ผู้รับผิดชอบ: นายจีรภัทร แก้วดี (673380577-9), branch `jiraphat_673380577-9_03`
Path ในตารางย่อจาก `code/src/main/java/com/example/roombooking/` บรรทัดอ้างอิงจาก `develop` commit `0c0b2ab`

## S: Single Responsibility

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `event/BookingStatusChangedEvent.java` | 8–21 | เป็นแค่ข้อมูลของเหตุการณ์ (booking, สถานะเดิม, สถานะใหม่, ผู้เปลี่ยน) ไม่มี logic |
| `event/NotificationListener.java` | 27–38 | รับ event แล้วส่งต่ออย่างเดียว ไม่บันทึกข้อมูลเอง |
| `service/impl/NotificationServiceImpl.java` | 25–44 | บันทึกประวัติและแจ้งเตือนอย่างเดียว ไม่รู้ว่า event มาจากไหน |
| `repository/BookingStatusHistoryRepository.java` | 8–11 | เข้าถึงตาราง `booking_status_history` อย่างเดียว |
| `config/JpaAuditingConfig.java` | 7–9 | เปิด JPA Auditing อย่างเดียว แยกจากคลาส main |
| `exception/GlobalExceptionHandler.java` | 27–176 | แปลง exception เป็น HTTP response ที่เดียว controller และ service ไม่ต้องจัดการ error เอง |
| `dto/response/ErrorResponse.java` | 6–29 | กำหนดรูปแบบ JSON ของ error อย่างเดียว |
| `config/SwaggerConfig.java` | 14–20 | ตั้งค่าเอกสาร API อย่างเดียว |

`BookingServiceImpl.updateStatus()` (บรรทัด 125–145) ไม่ต้องบันทึกประวัติหรือแจ้งเตือนเอง แค่ publish event (บรรทัด 141–142) งานเหล่านั้นเป็นหน้าที่ของ listener

## O: Open/Closed

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `service/impl/BookingServiceImpl.java` | 141–142 | publish `BookingStatusChangedEvent` โดยไม่รู้ว่ามี listener กี่ตัว |
| `event/NotificationListener.java` | 13–14, 27–29 | เป็น listener ตัวหนึ่ง ถ้าจะเพิ่มงาน (เช่น ส่งอีเมล, เก็บสถิติ) แค่ **เพิ่มคลาส** `@TransactionalEventListener` ใหม่ ไม่ต้อง **แก้** `BookingServiceImpl` |
| `service/impl/NotificationServiceImpl.java` | 35–43 | จุดต่อยอดช่องทางแจ้งเตือน เพิ่มอีเมล/SMS ได้โดยไม่แตะ listener หรือจุด publish |
| `exception/GlobalExceptionHandler.java` | 33–142 | รองรับ exception ชนิดใหม่โดย **เพิ่ม** method `@ExceptionHandler` ไม่ต้องแก้ controller (เช่น เพิ่ม handler ของ sort ผิดที่บรรทัด 100–117 โดยไม่แตะ controller ตัวไหนเลย) |

## L: Liskov Substitution

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `event/BookingStatusChangedEvent.java` | 8, 15–16 | `extends ApplicationEvent` และเรียก `super(source)` ใช้แทน `ApplicationEvent` ได้ทุกที่ที่ Spring ต้องการ |
| `service/impl/NotificationServiceImpl.java` | 14, 24–26 | implement `NotificationService` ครบ ไม่ throw `UnsupportedOperationException` รองรับ `changedBy == null` (ระบบเปลี่ยนเอง) ตามที่ interface คาดไว้ (บรรทัด 43) |
| `exception/GlobalExceptionHandler.java` | 149–155 | exception ของ Spring MVC ที่ implement `org.springframework.web.ErrorResponse` ถูกใช้ผ่าน interface เดียวกันทั้งหมด (404, 405, header ขาด) โดยไม่ต้องแยกเคสทีละคลาส |

## I: Interface Segregation

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `service/NotificationService.java` | 7–12 | มี method เดียวที่ listener ต้องใช้ ไม่รวมกับ `BookingService` |
| `repository/BookingStatusHistoryRepository.java` | 11 | เพิ่ม query เดียวที่จำเป็น (`findByBooking_IdOrderByChangedAtDesc`) |
| `event/NotificationListener.java` | 18 | รู้จักแค่ `NotificationService` ไม่ได้รับ `BookingService` หรือ repository ที่ไม่ใช้ |

## D: Dependency Inversion

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `service/impl/BookingServiceImpl.java` | 37, 45 | ขึ้นกับ `ApplicationEventPublisher` (abstraction ของ Spring) ไม่ได้เรียก `NotificationListener` ตรง ๆ |
| `event/NotificationListener.java` | 18–22 | ขึ้นกับ interface `NotificationService` ไม่ใช่ `NotificationServiceImpl` + constructor injection, field เป็น `final` |
| `service/impl/NotificationServiceImpl.java` | 18–22 | รับ `BookingStatusHistoryRepository` (interface) ผ่าน constructor |

ทิศทางการพึ่งพา:

```
BookingServiceImpl ──> ApplicationEventPublisher (abstraction)
                              │ (Spring ส่ง event)
NotificationListener ──> NotificationService (interface) <── NotificationServiceImpl
                                                                  └──> BookingStatusHistoryRepository (interface)
```

`BookingServiceImpl` กับ `NotificationListener` ไม่ import กันและกัน ทั้งสองฝั่งรู้จักแค่ `BookingStatusChangedEvent`

---

## Layered Architecture ของส่วนนี้

| Layer | คลาสของคนที่ 5 | ห้ามข้ามไป |
|---|---|---|
| Presentation | `GlobalExceptionHandler`, `ErrorResponse`, `SwaggerConfig` | ไม่เรียก repository |
| Service | `NotificationService`, `NotificationServiceImpl`, `NotificationListener`, `BookingStatusChangedEvent` | ไม่รู้จัก HTTP (ไม่มี `HttpServletRequest` หรือ `ResponseEntity`) |
| Repository | `BookingStatusHistoryRepository` | - |
| Domain | `BookingStatusHistory` | - |
| Config | `JpaAuditingConfig` | - |

ส่วนนี้ไม่มีการเรียกข้าม layer: controller → service → repository เสมอ และ exception ที่ service โยนขึ้นมาถูกแปลงเป็น HTTP ที่ Presentation layer เท่านั้น
