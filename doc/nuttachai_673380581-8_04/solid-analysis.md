# SOLID Analysis: ส่วน Booking Validation / Equipment Linking / Booking DTO และ Mapper

ผู้รับผิดชอบ: ณัฏฐชัย ผลดี (673380581-8), branch `nuttachai_673380581-8_04`
Path ในตารางย่อจาก `code/src/main/java/com/example/roombooking/` บรรทัดอ้างอิงโค้ดใน branch นี้ขณะจัดทำเอกสาร

ขอบเขตนี้ครอบคลุมงานของคนที่ 4 ส่วน `BookingServiceImpl`, Entity ของ Booking / Room / Equipment และการจัดการ HTTP error เป็นจุดเชื่อมต่อกับงานสมาชิกอื่น

## S: Single Responsibility

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| [dto/request/BookingCreateRequest.java](../../code/src/main/java/com/example/roombooking/dto/request/BookingCreateRequest.java) | 18–39, 118–125 | กำหนดข้อมูลขาเข้าและตรวจรูปแบบด้วย `@NotNull`, `@Size`, `@Valid`, `@Positive` โดยกฎที่ต้องอ่านฐานข้อมูลแยกไปอยู่ใน Handler |
| [service/validation/UserPermissionHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/UserPermissionHandler.java) | 15–35 | ตรวจสิทธิ์บัญชีและการจองแทนผู้อื่น ไม่มีหน้าที่บันทึก Booking หรือคำนวณอุปกรณ์ |
| [service/validation/RoomAvailabilityHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/RoomAvailabilityHandler.java) | 27–43 | ตรวจว่ามีห้องและห้องอยู่สถานะ `AVAILABLE` แล้วเก็บห้องใน Context ให้ส่วนถัดไปใช้ |
| [service/validation/TimeOverlapHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/TimeOverlapHandler.java) | 32–59 | ตรวจช่วงเวลาและการจองห้องซ้อนทับ โดยตัด Booking เดิมออกเมื่อแก้ไขรายการ |
| [service/validation/EquipmentAvailabilityHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/EquipmentAvailabilityHandler.java) | 34–75 | รวมจำนวนอุปกรณ์ที่ขอซ้ำและตรวจจำนวนคงเหลือ ไม่มีหน้าที่เปลี่ยนสถานะหรือบันทึกการจอง |
| [mapper/BookingMapper.java](../../code/src/main/java/com/example/roombooking/mapper/BookingMapper.java) | 18–73 | แปลง Request → Entity และ Entity → Response รวมถึงรายการอุปกรณ์ ทำให้รูปแบบข้อมูลตอบกลับแยกจากการตรวจสิทธิ์และฐานข้อมูล |
| [domain/entity/BookingEquipment.java](../../code/src/main/java/com/example/roombooking/domain/entity/BookingEquipment.java) | 16–39 | แทนข้อมูลการเชื่อม Booking กับ Equipment พร้อม `quantity`, FK, index และข้อจำกัดจำนวนบวก ไม่รับหน้าที่เป็น Controller หรือ Validator |

เหตุผลของการแยกกฎเป็นหลาย Handler คือเมื่อเปลี่ยนนโยบายการจองแทนผู้อื่น จะปรับเฉพาะ `UserPermissionHandler` โดยไม่ต้องแก้โค้ดตรวจจำนวนอุปกรณ์

## O: Open/Closed

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| [service/validation/BookingValidationHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/BookingValidationHandler.java) | 7–12 | กำหนดจุดขยาย `doValidate(context)` ให้เพิ่มกฎผ่านคลาสลูก โดย `handle(context)` ยังคงเรียกตามสัญญาเดียวกัน |
| [service/validation/BookingValidationChain.java](../../code/src/main/java/com/example/roombooking/service/validation/BookingValidationChain.java) | 7–22 | รับ Handler เป็น List ผ่าน Spring และเรียกผ่านคลาสฐาน ไม่มี `if` แยกตามชื่อ Handler |
| [service/validation/EquipmentAvailabilityHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/EquipmentAvailabilityHandler.java) | 19–21, 34 | เป็นตัวอย่างกฎที่เพิ่มผ่าน `@Component`, `@Order(4)` และ override `doValidate` โดยไม่ต้องเพิ่มเงื่อนไขเฉพาะใน Chain |

หากเพิ่มกฎตรวจระยะเวลาจอง สามารถสร้าง Handler ใหม่และกำหนดลำดับที่เหมาะสมได้ โดยไม่แก้ลูปใน `BookingValidationChain` อย่างไรก็ตาม หากกฎใหม่ต้องการข้อมูลที่ Context ยังไม่มี อาจต้องขยาย Context และจุดสร้างข้อมูลด้วย จึงไม่ได้หมายความว่าทุกการเปลี่ยนแปลงจะทำได้โดยเพิ่มคลาสอย่างเดียว

## L: Liskov Substitution

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| [service/validation/BookingValidationHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/BookingValidationHandler.java) | 7–12 | สัญญาของ Handler คือรับ Context ตรวจเฉพาะกฎของตน และคืนตามปกติเมื่อผ่าน หรือ throw exception เมื่อไม่ผ่าน |
| [service/validation/UserPermissionHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/UserPermissionHandler.java) | 15–35 | ใช้ผ่านชนิด `BookingValidationHandler` ได้ และแจ้งการไม่ผ่านด้วย `ForbiddenException` ตามสัญญาของการตรวจสอบ |
| [service/validation/TimeOverlapHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/TimeOverlapHandler.java) | 20, 32–59 | ใช้สัญญาเดียวกันโดยตรวจเวลาซ้อนทับ ไม่เพิ่ม method ที่ Chain ต้องเรียกเป็นพิเศษ |
| [service/validation/BookingValidationChain.java](../../code/src/main/java/com/example/roombooking/service/validation/BookingValidationChain.java) | 20–22 | ใช้ Handler ทุกตัวผ่าน `handle(context)` ไม่ cast ไปยังคลาสลูกหรือเช็กชนิดก่อนเรียก |

การใช้คลาสลูกผ่านคลาสฐานได้ไม่ได้หมายความว่านำกฎหนึ่งไปแทนอีกกฎแล้วได้ผลทางธุรกิจเหมือนกัน หรือนำมาสลับลำดับได้อิสระ ลำดับปัจจุบันคือ สิทธิ์ผู้ใช้ → สถานะห้อง → เวลาซ้อนทับ → อุปกรณ์ ซึ่งทำให้การตรวจอุปกรณ์เกิดหลังช่วงเวลาถูกตรวจแล้ว ส่วน exception จากกฎที่ไม่ผ่านทำให้ Chain หยุดและไม่เรียกกฎถัดไป

## I: Interface Segregation

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| [service/validation/BookingValidationHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/BookingValidationHandler.java) | 7–12 | แม้เป็น abstract class ไม่ใช่ Java interface แต่บังคับให้คลาสลูก implement เพียง `doValidate` ไม่ต้องมี method สำหรับสร้าง ลบ หรืออนุมัติ Booking |
| [repository/BookingEquipmentRepository.java](../../code/src/main/java/com/example/roombooking/repository/BookingEquipmentRepository.java) | 83–89 | Projection `ReservationWindow` เปิดเพียงเวลาเริ่ม เวลาสิ้นสุด และจำนวน ซึ่งเป็นข้อมูลที่อัลกอริทึมคำนวณจำนวนจองต้องใช้ |
| [dto/request/BookingCreateRequest.java](../../code/src/main/java/com/example/roombooking/dto/request/BookingCreateRequest.java) และ [dto/response/BookingResponse.java](../../code/src/main/java/com/example/roombooking/dto/response/BookingResponse.java) | 18–39, 118–125 / 10–23, 129–137 | แยกข้อมูลรับกับข้อมูลตอบกลับ ผู้ส่งรายการอุปกรณ์ระบุเพียง ID และจำนวน ขณะที่ Response เพิ่มชื่ออุปกรณ์เพื่อแสดงผล จึงไม่ต้องใช้ Entity ทั้งก้อนเป็นสัญญาข้อมูล |

DTO ที่แยกกันช่วยลดขอบเขตข้อมูล แต่ไม่ใช่หลักฐานของ ISP เพียงอย่างเดียว หลักฐานที่ชัดในส่วนนี้คือสัญญา Handler ที่เล็กและ Projection ที่มีเฉพาะ getter ที่จำเป็น ส่วน Repository ยังสืบทอด method CRUD จาก `JpaRepository` จึงไม่ได้แยกเป็น interface เฉพาะทุกผู้ใช้งาน

## D: Dependency Inversion

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| [service/validation/BookingValidationChain.java](../../code/src/main/java/com/example/roombooking/service/validation/BookingValidationChain.java) | 10–14, 20–22 | ถือ `List<BookingValidationHandler>` และรับผ่าน constructor จึงไม่ต้อง `new` Handler แต่ละคลาสหรือผูกกับชื่อ implementation |
| [service/validation/RoomAvailabilityHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/RoomAvailabilityHandler.java) | 19–24 | รับ `MeetingRoomRepository` ซึ่งเป็น interface ผ่าน constructor ทำให้ทดสอบกฎด้วย mock ได้โดยไม่ต้องเปิดฐานข้อมูลจริง |
| [service/validation/TimeOverlapHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/TimeOverlapHandler.java) | 24–29 | รับ `BookingRepository` ผ่าน constructor เพื่อแยกการตรวจเวลาจากวิธีเข้าถึงข้อมูล |
| [service/validation/EquipmentAvailabilityHandler.java](../../code/src/main/java/com/example/roombooking/service/validation/EquipmentAvailabilityHandler.java) | 23–31, 65–67 | รับ Repository interface ทั้งสองผ่าน constructor แล้วเรียก `sumReservedQuantity` เพื่อรับจำนวนที่จอง แทนการเขียน query ใน Handler |
| [repository/BookingEquipmentRepository.java](../../code/src/main/java/com/example/roombooking/repository/BookingEquipmentRepository.java) | 13, 27–69 | ซ่อนรายละเอียด query และการคำนวณจำนวนจองพร้อมกันสูงสุดไว้หลัง Repository ทำให้ Handler สนใจเพียง `available = totalQuantity - reserved` |

ตัวอย่างเช่น อุปกรณ์ทั้งหมด 5 ชิ้น และจำนวนที่ถูกจองพร้อมกันสูงสุดเป็น 3 ชิ้น Handler จะตรวจว่าจำนวนที่ขอไม่เกิน 2 ชิ้น โดยไม่ต้องรู้รายละเอียด `TreeMap` ที่ Repository ใช้รวมเหตุการณ์เริ่มและจบการจอง

การใช้ constructor injection และ interface ช่วยลดการผูกกับ implementation แต่ยังไม่ใช่การแยกจาก framework ทั้งหมด เพราะ Repository สืบทอด `JpaRepository` ใช้ JPQL และ Context อ้างถึง JPA Entity อยู่ หากเปลี่ยนระบบจัดเก็บข้อมูลครั้งใหญ่ยังต้องปรับส่วนเชื่อมต่อเหล่านี้
