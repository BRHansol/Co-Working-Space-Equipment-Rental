package com.example.roombooking.service;

import com.example.roombooking.common.PageResponse;
import com.example.roombooking.dto.request.UserCreateRequest;
import com.example.roombooking.dto.request.UserUpdateRequest;
import com.example.roombooking.dto.response.UserResponse;

public interface UserService {
    UserResponse getUserById(Long id);
    UserResponse createUser(UserCreateRequest request);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    PageResponse<UserResponse> getAllUsers(int page, int size);
    boolean exitsById(Long id);
    void deleteUserById(Long id);
}
