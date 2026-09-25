package com.example.roombooking.controller.api;

import java.time.LocalDate;

import com.example.roombooking.common.PageResponse;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.dto.request.UserCreateRequest;
import com.example.roombooking.dto.request.UserUpdateRequest;
import com.example.roombooking.dto.response.UserResponse;
import com.example.roombooking.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing user accounts and profiles")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new user profile")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());
        user.setCreated_at(LocalDate.now());

        User created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(mapToResponse(user));
    }

    @GetMapping
    @Operation(summary = "Get list of users with pagination and sorting")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<User> response = userService.getAllUsers(pageable.getPageNumber(), pageable.getPageSize());
        PageResponse<UserResponse> mapped = PageResponse.<UserResponse>builder()
                .content(response.getContent().stream().map(this::mapToResponse).toList())
                .pageNo(response.getPageNo())
                .pageSize(response.getPageSize())
                .totalElements(response.getTotalElements())
                .totalPages(response.getTotalPages())
                .last(response.isLast())
                .build();
        return ResponseEntity.ok(mapped);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user details")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        User user = userService.getUserById(id);
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        return ResponseEntity.ok(mapToResponse(user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user account")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    private UserResponse mapToResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .fullName(user.getUsername())
                .createdAt(user.getCreated_at() != null ? user.getCreated_at().atStartOfDay() : null)
                .build();
    }
}