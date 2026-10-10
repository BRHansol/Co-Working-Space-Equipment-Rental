package com.example.roombooking.service.impl;

import com.example.roombooking.common.PageResponse;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.dto.request.UserCreateRequest;
import com.example.roombooking.dto.request.UserUpdateRequest;
import com.example.roombooking.dto.response.UserResponse;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.UserMapper;
import com.example.roombooking.repository.UserRepository;
import com.example.roombooking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String encoded = passwordEncoder.encode(request.getPassword());
        User user = userMapper.toEntity(request, encoded);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot update. User not found with id: " + id));
        userMapper.updateEntity(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> userPage = userRepository.findAll(pageable);

        return PageResponse.<UserResponse>builder()
                .content(userMapper.toResponseList(userPage.getContent()))
                .pageNo(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .build();
    }

    @Override
    public boolean exitsById(Long id) {
        return userRepository.existsById(id);
    }

    @Override
    @Transactional
    public void deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}