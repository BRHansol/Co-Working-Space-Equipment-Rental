package com.example.roombooking.mapper;

import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.entity.UserProfile;
import com.example.roombooking.dto.request.UserCreateRequest;
import com.example.roombooking.dto.request.UserUpdateRequest;
import com.example.roombooking.dto.response.UserResponse;
import org.springframework.stereotype.Component;
 
import java.time.LocalDate;
import java.util.List;
 
@Component
public class UserMapper {
     public User toEntity(UserCreateRequest request, String encodedPassword) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(encodedPassword);
        user.setRole(request.getRole());
        user.setActive(true);
        user.setCreated_at(LocalDate.now());
 
        UserProfile profile = new UserProfile();
        profile.setFullName(request.getFullName());
        profile.setPhone(request.getPhone());
        profile.setDepartment(request.getDepartment());
 
        // setProfile() ผูก relation กลับ (profile.setUser(this)) ให้อัตโนมัติอยู่แล้ว
        // ตามที่เขียนไว้ใน User.java
        user.setProfile(profile);
 
        return user;
    }
 
    /**
     * อัปเดตค่าของ User + UserProfile เดิมจาก request (เฉพาะ field ที่ไม่เป็น null)
     * ไม่แตะ username / password / role ตรงนี้ เพราะ UserUpdateRequest ไม่มี field พวกนี้
     * (ควรมี endpoint แยกต่างหากสำหรับเปลี่ยนรหัสผ่าน หรือเปลี่ยน role โดย ADMIN เท่านั้น)
     */
    public void updateEntity(User user, UserUpdateRequest request) {
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
 
        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile();
            user.setProfile(profile);
        }
        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }
        if (request.getDepartment() != null) {
            profile.setDepartment(request.getDepartment());
        }
    }
 
    public UserResponse toResponse(User user) {
        UserProfile profile = user.getProfile();
 
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .fullName(profile != null ? profile.getFullName() : null)
                .phone(profile != null ? profile.getPhone() : null)
                .department(profile != null ? profile.getDepartment() : null)
                .createdAt(user.getCreated_at() != null ? user.getCreated_at().atStartOfDay() : null)
                .build();
    }
 
    public List<UserResponse> toResponseList(List<User> users) {
        return users.stream().map(this::toResponse).toList();
    }
}
