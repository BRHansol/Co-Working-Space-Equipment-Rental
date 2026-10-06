package com.example.roombooking.controller.web.forms;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {
    @NotBlank(message = "กรอกชื่อผู้ใช้")
    private String username;

    @NotBlank(message = "กรอกรหัสผ่าน")
    private String password;
}
