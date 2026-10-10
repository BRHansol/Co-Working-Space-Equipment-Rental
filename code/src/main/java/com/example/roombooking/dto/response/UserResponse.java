package com.example.roombooking.dto.response;

import java.time.LocalDateTime;

import com.example.roombooking.domain.enums.Role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private Role role;

    // Profile detail
    private String fullName;
    private String phone;
    private String department;

    private LocalDateTime createdAt;
}