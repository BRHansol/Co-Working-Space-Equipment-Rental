package com.example.roombooking.controller.web.form;
import com.example.roombooking.domain.enums.*;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class AdminRoomForm {
    @NotBlank(message="กรุณาระบุชื่อห้อง") @Size(max=100) private String name;
    @NotNull @Min(value=1,message="ความจุต้องอย่างน้อย 1 คน") private Integer capacity;
    @NotBlank(message="กรุณาระบุชั้น") @Size(max=20) private String floor;
    @NotNull private RoomType roomType = RoomType.STANDARD;
    @NotNull private RoomStatus status = RoomStatus.AVAILABLE;
}
