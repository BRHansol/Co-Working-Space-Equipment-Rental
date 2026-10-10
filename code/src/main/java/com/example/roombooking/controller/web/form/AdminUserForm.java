package com.example.roombooking.controller.web.form;
import com.example.roombooking.domain.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class AdminUserForm {
    @NotBlank @Size(min=4,max=50,message="ชื่อผู้ใช้ต้องมี 4–50 ตัวอักษร") private String username;
    @NotBlank @Email(message="รูปแบบอีเมลไม่ถูกต้อง") @Size(max=254) private String email;
    @NotBlank @Size(min=6,max=72,message="รหัสผ่านต้องมี 6–72 ตัวอักษร") private String password;
    @NotNull private Role role = Role.USER;
}
