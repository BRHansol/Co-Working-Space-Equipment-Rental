package com.example.roombooking.controller.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.roombooking.common.PageResponse;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.dto.request.UserCreateRequest;
import com.example.roombooking.dto.request.UserUpdateRequest;
import com.example.roombooking.dto.response.UserResponse;
import com.example.roombooking.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

class UserControllerTest {

    private UserService userService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userController = new UserController(userService);
    }

    @Test
    void createUserCreatesUserAndReturnsCreatedResponse() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("alex-user");
        request.setEmail("alex@example.com");
        request.setPassword("secret123");
        request.setRole(Role.USER);
        request.setFullName("Alex User");
        when(userService.createUser(request)).thenReturn(user(12L,"alex-user","alex@example.com",Role.USER,LocalDate.now()));

        var response = userController.createUser(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(12L, response.getBody().getId());
        assertEquals("alex-user", response.getBody().getUsername());
        assertEquals("alex@example.com", response.getBody().getEmail());
        assertEquals(Role.USER, response.getBody().getRole());
        assertEquals("Alex User", response.getBody().getFullName());
        assertEquals(LocalDate.now().atStartOfDay(), response.getBody().getCreatedAt());
        verify(userService).createUser(request);
    }

    @Test
    void getUserByIdReturnsMappedUser() {
        UserResponse user = user(12L, "alex-user", "alex@example.com", Role.ADMIN, LocalDate.of(2025, 1, 10));
        when(userService.getUserById(12L)).thenReturn(user);

        var response = userController.getUserById(12L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(12L, response.getBody().getId());
        assertEquals("alex-user", response.getBody().getUsername());
        assertEquals(Role.ADMIN, response.getBody().getRole());
        assertEquals(LocalDateTime.of(2025, 1, 10, 0, 0), response.getBody().getCreatedAt());
        verify(userService).getUserById(12L);
    }

    @Test
    void getAllUsersMapsContentAndPagination() {
        UserResponse user = user(12L, "alex-user", "alex@example.com", Role.USER, LocalDate.of(2025, 1, 10));
        PageResponse<UserResponse> page = PageResponse.<UserResponse>builder()
                .content(List.of(user))
                .pageNo(2)
                .pageSize(5)
                .totalElements(11)
                .totalPages(3)
                .last(false)
                .build();
        when(userService.getAllUsers(2, 5)).thenReturn(page);

        var response = userController.getAllUsers(PageRequest.of(2, 5));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        UserResponse mappedUser = response.getBody().getContent().get(0);
        assertEquals(12L, mappedUser.getId());
        assertEquals("Alex User", mappedUser.getFullName());
        assertEquals(2, response.getBody().getPageNo());
        assertEquals(5, response.getBody().getPageSize());
        assertEquals(11, response.getBody().getTotalElements());
        assertEquals(3, response.getBody().getTotalPages());
        assertEquals(false, response.getBody().isLast());
        verify(userService).getAllUsers(2, 5);
    }

    @Test
    void updateUserChangesEmailAndKeepsOtherFields() {
        UserResponse user = user(12L, "alex-user", "new@example.com", Role.USER, LocalDate.of(2025, 1, 10));
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("new@example.com");
        request.setFullName("Alex User");
        when(userService.updateUser(12L,request)).thenReturn(user);

        var response = userController.updateUser(12L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("new@example.com", response.getBody().getEmail());
        assertEquals("Alex User", response.getBody().getFullName());
        verify(userService).updateUser(12L,request);
    }

    @Test
    void updateUserWithNullEmailKeepsExistingEmail() {
        UserResponse user = user(12L, "alex-user", "alex@example.com", Role.USER, null);
        UserUpdateRequest request=new UserUpdateRequest();
        when(userService.updateUser(12L,request)).thenReturn(user);

        var response = userController.updateUser(12L, request);

        assertNotNull(response.getBody());
        assertEquals("alex@example.com", response.getBody().getEmail());
        assertNull(response.getBody().getCreatedAt());
        verify(userService).updateUser(12L,request);
    }

    @Test
    void deleteUserDeletesAccountAndReturnsNoContent() {
        var response = userController.deleteUser(12L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService).deleteUserById(12L);
    }

    private UserResponse user(Long id, String username, String email, Role role, LocalDate createdAt) {
        return UserResponse.builder().id(id).username(username).email(email).role(role).fullName("Alex User")
                .createdAt(createdAt==null ? null : createdAt.atStartOfDay()).build();
    }
}
