package com.example.roombooking.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Email(message = "Invalid email format")
    private String email;

    private String fullName;
    private String phone;
    private String department;
}