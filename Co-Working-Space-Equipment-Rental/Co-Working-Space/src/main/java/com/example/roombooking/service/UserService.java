package com.example.roombooking.service;
import com.example.roombooking.common.PageResponse;
import com.example.roombooking.domain.entity.User;

public interface UserService {
    User getUserById(Long id);
    User getUserByUserName(String username);
    User createUser(User user);
    PageResponse<User> getAllUsers(int page, int size);
    boolean exitsById(Long id);
    void deleteUserById(Long id);
}
