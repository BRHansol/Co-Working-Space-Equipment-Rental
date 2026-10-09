package com.example.roombooking.controller.web.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationForm {
    @NotBlank(message = "กรอกชื่อผู้ใช้")
    @Size(min = 4, max = 50, message = "ชื่อผู้ใช้ต้องมี 4–50 ตัวอักษร")
    private String username;

    @NotBlank(message = "กรอกอีเมล")
    @Email(message = "กรอกอีเมลในรูปแบบ name@example.com")
    @Size(max = 254, message = "อีเมลต้องไม่เกิน 254 ตัวอักษร")
    private String email;

    @NotBlank(message = "กรอกรหัสผ่าน")
    @Size(min = 6, max = 72, message = "รหัสผ่านต้องมี 6–72 ตัวอักษร")
    private String password;

    @NotBlank(message = "กรอกรหัสผ่านอีกครั้ง")
    private String confirmPassword;
}
