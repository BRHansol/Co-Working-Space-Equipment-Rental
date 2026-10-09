package com.example.roombooking.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.example.roombooking.common.PageResponse;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.dto.request.UserCreateRequest;
import com.example.roombooking.dto.request.UserUpdateRequest;
import com.example.roombooking.dto.response.UserResponse;
import com.example.roombooking.exception.ResourceNotFoundException;
import com.example.roombooking.mapper.UserMapper;
import com.example.roombooking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper, passwordEncoder);
    }

    private User buildUser(Long id, String username, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encodedPassword123");
        user.setRole(role);
        user.setActive(true);
        user.setCreated_at(LocalDate.now());
        return user;
    }

    private UserResponse buildUserResponse(Long id, String username, String email, Role role) {
        return UserResponse.builder()
                .id(id)
                .username(username)
                .email(email)
                .role(role)
                .fullName("Test User")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getUserById Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("getUserById returns UserResponse when user exists")
        void getUserById_whenUserExists_shouldReturnUserResponse() {
            Long userId = 1L;
            User user = buildUser(userId, "testuser", "test@example.com", Role.USER);
            UserResponse expectedResponse = buildUserResponse(userId, "testuser", "test@example.com", Role.USER);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(expectedResponse);

            UserResponse actualResponse = userService.getUserById(userId);

            assertNotNull(actualResponse);
            assertEquals(expectedResponse.getId(), actualResponse.getId());
            assertEquals(expectedResponse.getUsername(), actualResponse.getUsername());
            assertEquals(expectedResponse.getEmail(), actualResponse.getEmail());
            assertEquals(expectedResponse.getRole(), actualResponse.getRole());

            verify(userRepository).findById(userId);
            verify(userMapper).toResponse(user);
        }

        @Test
        @DisplayName("getUserById throws ResourceNotFoundException when user is not found")
        void getUserById_whenUserNotFound_shouldThrowResourceNotFoundException() {
            Long userId = 99L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.getUserById(userId)
            );

            assertEquals("User not found with id: " + userId, exception.getMessage());
            verify(userRepository).findById(userId);
            verify(userMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("createUser Tests")
    class CreateUserTests {

        @Test
        @DisplayName("createUser encodes password, maps entity, saves and returns UserResponse")
        void createUser_whenValidRequest_shouldEncodePasswordSaveAndReturnResponse() {
            UserCreateRequest request = new UserCreateRequest();
            request.setUsername("newuser");
            request.setEmail("newuser@example.com");
            request.setPassword("plainPassword");
            request.setRole(Role.USER);
            request.setFullName("New User");

            String encodedPassword = "encodedPassword123";
            User mappedUser = buildUser(null, "newuser", "newuser@example.com", Role.USER);
            User savedUser = buildUser(1L, "newuser", "newuser@example.com", Role.USER);
            UserResponse expectedResponse = buildUserResponse(1L, "newuser", "newuser@example.com", Role.USER);

            when(passwordEncoder.encode("plainPassword")).thenReturn(encodedPassword);
            when(userMapper.toEntity(request, encodedPassword)).thenReturn(mappedUser);
            when(userRepository.save(mappedUser)).thenReturn(savedUser);
            when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

            UserResponse actualResponse = userService.createUser(request);

            assertNotNull(actualResponse);
            assertEquals(1L, actualResponse.getId());
            assertEquals("newuser", actualResponse.getUsername());
            assertEquals("newuser@example.com", actualResponse.getEmail());
            assertEquals(Role.USER, actualResponse.getRole());

            verify(passwordEncoder).encode("plainPassword");
            verify(userMapper).toEntity(request, encodedPassword);
            verify(userRepository).save(mappedUser);
            verify(userMapper).toResponse(savedUser);
        }
    }

    @Nested
    @DisplayName("updateUser Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("updateUser updates user fields, saves and returns UserResponse when user exists")
        void updateUser_whenUserExists_shouldUpdateSaveAndReturnResponse() {
            Long userId = 1L;
            UserUpdateRequest request = new UserUpdateRequest();
            request.setEmail("updated@example.com");
            request.setFullName("Updated Name");

            User existingUser = buildUser(userId, "testuser", "test@example.com", Role.USER);
            User savedUser = buildUser(userId, "testuser", "updated@example.com", Role.USER);
            UserResponse expectedResponse = buildUserResponse(userId, "testuser", "updated@example.com", Role.USER);

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(existingUser)).thenReturn(savedUser);
            when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

            UserResponse actualResponse = userService.updateUser(userId, request);

            assertNotNull(actualResponse);
            assertEquals(expectedResponse.getEmail(), actualResponse.getEmail());

            verify(userRepository).findById(userId);
            verify(userMapper).updateEntity(existingUser, request);
            verify(userRepository).save(existingUser);
            verify(userMapper).toResponse(savedUser);
        }

        @Test
        @DisplayName("updateUser throws ResourceNotFoundException when user does not exist")
        void updateUser_whenUserNotFound_shouldThrowResourceNotFoundException() {
            Long userId = 99L;
            UserUpdateRequest request = new UserUpdateRequest();
            request.setEmail("updated@example.com");

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.updateUser(userId, request)
            );

            assertEquals("Cannot update. User not found with id: " + userId, exception.getMessage());
            verify(userRepository).findById(userId);
            verify(userMapper, never()).updateEntity(any(), any());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllUsers Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("getAllUsers returns populated PageResponse sorted by id descending")
        void getAllUsers_whenUsersExist_shouldReturnPopulatedPageResponse() {
            int page = 0;
            int size = 5;
            Pageable expectedPageable = PageRequest.of(page, size, Sort.by("id").descending());

            User user1 = buildUser(1L, "user1", "user1@example.com", Role.USER);
            User user2 = buildUser(2L, "user2", "user2@example.com", Role.ADMIN);
            List<User> userList = List.of(user2, user1);

            Page<User> userPage = new PageImpl<>(userList, expectedPageable, 2);

            UserResponse response1 = buildUserResponse(1L, "user1", "user1@example.com", Role.USER);
            UserResponse response2 = buildUserResponse(2L, "user2", "user2@example.com", Role.ADMIN);
            List<UserResponse> responseList = List.of(response2, response1);

            when(userRepository.findAll(expectedPageable)).thenReturn(userPage);
            when(userMapper.toResponseList(userList)).thenReturn(responseList);

            PageResponse<UserResponse> result = userService.getAllUsers(page, size);

            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            assertEquals(0, result.getPageNo());
            assertEquals(5, result.getPageSize());
            assertEquals(2L, result.getTotalElements());
            assertEquals(1, result.getTotalPages());
            assertTrue(result.isLast());

            verify(userRepository).findAll(expectedPageable);
            verify(userMapper).toResponseList(userList);
        }

        @Test
        @DisplayName("getAllUsers returns empty PageResponse when no users exist")
        void getAllUsers_whenEmpty_shouldReturnEmptyPageResponse() {
            int page = 0;
            int size = 10;
            Pageable expectedPageable = PageRequest.of(page, size, Sort.by("id").descending());

            Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), expectedPageable, 0);

            when(userRepository.findAll(expectedPageable)).thenReturn(emptyPage);
            when(userMapper.toResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

            PageResponse<UserResponse> result = userService.getAllUsers(page, size);

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
            assertEquals(0, result.getPageNo());
            assertEquals(10, result.getPageSize());
            assertEquals(0L, result.getTotalElements());
            assertEquals(0, result.getTotalPages());
            assertTrue(result.isLast());

            verify(userRepository).findAll(expectedPageable);
            verify(userMapper).toResponseList(Collections.emptyList());
        }
    }

    @Nested
    @DisplayName("exitsById Tests")
    class ExitsByIdTests {

        @Test
        @DisplayName("exitsById returns true when user exists")
        void exitsById_whenUserExists_shouldReturnTrue() {
            Long userId = 1L;
            when(userRepository.existsById(userId)).thenReturn(true);

            boolean result = userService.exitsById(userId);

            assertTrue(result);
            verify(userRepository).existsById(userId);
        }

        @Test
        @DisplayName("exitsById returns false when user does not exist")
        void exitsById_whenUserDoesNotExist_shouldReturnFalse() {
            Long userId = 99L;
            when(userRepository.existsById(userId)).thenReturn(false);

            boolean result = userService.exitsById(userId);

            assertFalse(result);
            verify(userRepository).existsById(userId);
        }
    }

    @Nested
    @DisplayName("deleteUserById Tests")
    class DeleteUserByIdTests {

        @Test
        @DisplayName("deleteUserById deletes user when user exists")
        void deleteUserById_whenUserExists_shouldDeleteUser() {
            Long userId = 1L;
            when(userRepository.existsById(userId)).thenReturn(true);

            userService.deleteUserById(userId);

            verify(userRepository).existsById(userId);
            verify(userRepository).deleteById(userId);
        }

        @Test
        @DisplayName("deleteUserById throws ResourceNotFoundException when user does not exist")
        void deleteUserById_whenUserNotFound_shouldThrowResourceNotFoundException() {
            Long userId = 99L;
            when(userRepository.existsById(userId)).thenReturn(false);

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.deleteUserById(userId)
            );

            assertEquals("Cannot delete. User not found with id: " + userId, exception.getMessage());
            verify(userRepository).existsById(userId);
            verify(userRepository, never()).deleteById(any());
        }
    }
}
