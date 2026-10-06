package com.example.roombooking.controller.web.form;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class AdminEquipmentForm {
    @NotBlank(message="กรุณาระบุชื่ออุปกรณ์") @Size(max=100) private String name;
    @NotNull @Min(value=0,message="จำนวนต้องไม่น้อยกว่า 0") private Integer totalQuantity;
    @NotBlank(message="กรุณาระบุหมวดหมู่") @Size(max=50) private String category;
}
