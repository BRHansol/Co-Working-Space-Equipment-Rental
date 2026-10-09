package com.example.roombooking.controller.web;

import com.example.roombooking.controller.web.support.WebAccessInterceptor;
import com.example.roombooking.controller.web.support.WebSessionSupport;
import com.example.roombooking.controller.web.support.WebViewAdvice;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.dto.request.UserCreateRequest;
import com.example.roombooking.dto.response.UserResponse;
import com.example.roombooking.repository.UserRepository;
import com.example.roombooking.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WebAuthFlowTest {
    @Mock private UserRepository users;
    @Mock private UserService userService;
    private WebSessionSupport sessions;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        sessions = new WebSessionSupport(users);
        mvc = MockMvcBuilders.standaloneSetup(new AuthViewController(users, userService, sessions),
                        new ProtectedPages())
                .setControllerAdvice(new WebViewAdvice(sessions))
                .addInterceptors(new WebAccessInterceptor(sessions)).build();
    }

    @Test
    void anonymousBookingAccessRedirectsToLogin() throws Exception {
        mvc.perform(get("/bookings")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?next=%2Fbookings"));
    }

    @Test
    void loginRejectsMissingCsrfBeforeReadingCredentials() throws Exception {
        mvc.perform(post("/login").param("username", "narin").param("password", "local123"))
                .andExpect(status().isForbidden()).andExpect(view().name("common/error"));
        verifyNoInteractions(users, userService);
    }

    @Test
    void correctPasswordRotatesSessionAndRejectsExternalRedirect() throws Exception {
        User user = user(1L, "narin", Role.USER);
        user.setPassword(sessions.hashPassword("local123"));
        when(users.findByUsername("narin")).thenReturn(Optional.of(user));
        MockHttpSession session = anonymousSession();
        String previousId = session.getId();
        mvc.perform(post("/login").session(session).param("_csrf", "test-csrf")
                        .param("username", "narin").param("password", "local123")
                        .param("next", "https://example.invalid"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/account"));
        assertEquals(1L, session.getAttribute(WebSessionSupport.USER_ID_SESSION_KEY));
        assertNotEquals(previousId, session.getId());
        assertNotEquals("test-csrf", session.getAttribute(WebSessionSupport.CSRF_SESSION_KEY));
    }

    @Test
    void incorrectPasswordDoesNotAuthenticate() throws Exception {
        User user = user(1L, "narin", Role.USER);
        user.setPassword(sessions.hashPassword("local123"));
        when(users.findByUsername("narin")).thenReturn(Optional.of(user));
        MockHttpSession session = anonymousSession();
        mvc.perform(post("/login").session(session).param("_csrf", "test-csrf")
                        .param("username", "narin").param("password", "wrong-password"))
                .andExpect(status().isOk()).andExpect(view().name("auth/login"))
                .andExpect(model().attributeHasErrors("loginForm"));
        assertNull(session.getAttribute(WebSessionSupport.USER_ID_SESSION_KEY));
    }

    @Test
    void registrationPassesRawPasswordAndUserRoleToServiceAndUsesSavedEntity() throws Exception {
        when(users.findAll()).thenReturn(List.of());
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(UserResponse.builder().id(42L).build());
        User saved = user(42L, "newuser", Role.USER);
        saved.setPassword(sessions.hashPassword("local123"));
        when(users.findById(42L)).thenReturn(Optional.of(saved));
        MockHttpSession session = anonymousSession();
        mvc.perform(post("/register").session(session).param("_csrf", "test-csrf")
                        .param("username", "newuser").param("email", "new@example.com")
                        .param("password", "local123").param("confirmPassword", "local123")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/account"));
        ArgumentCaptor<UserCreateRequest> created = ArgumentCaptor.forClass(UserCreateRequest.class);
        verify(userService).createUser(created.capture());
        assertEquals(Role.USER, created.getValue().getRole());
        assertEquals("local123", created.getValue().getPassword());
        verify(users).findById(42L);
        assertEquals(42L, session.getAttribute(WebSessionSupport.USER_ID_SESSION_KEY));
    }

    @Test
    void duplicateUsernameRegistrationDoesNotSave() throws Exception {
        when(users.findAll()).thenReturn(List.of(user(1L, "existing", Role.USER)));
        mvc.perform(post("/register").session(anonymousSession()).param("_csrf", "test-csrf")
                        .param("username", "EXISTING").param("email", "new@example.com")
                        .param("password", "local123").param("confirmPassword", "local123"))
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "username"));
        verifyNoInteractions(userService);
    }

    @Test
    void ordinaryUserCannotOpenAdmin() throws Exception {
        when(users.findById(1L)).thenReturn(Optional.of(user(1L, "narin", Role.USER)));
        mvc.perform(get("/admin").session(authenticatedSession(1L)))
                .andExpect(status().isForbidden()).andExpect(view().name("common/error"));
    }

    @Test
    void staffMayManageBookingsButCannotManageUsers() throws Exception {
        when(users.findById(2L)).thenReturn(Optional.of(user(2L, "staff", Role.STAFF)));
        MockHttpSession session = authenticatedSession(2L);
        mvc.perform(get("/admin").session(session)).andExpect(status().isOk());
        mvc.perform(get("/admin/users").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/admin;ignored/users").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void inactiveAccountLosesExistingSessionAccess() throws Exception {
        User inactive = user(1L, "narin", Role.USER);
        inactive.setActive(false);
        when(users.findById(1L)).thenReturn(Optional.of(inactive));
        MockHttpSession session = authenticatedSession(1L);
        mvc.perform(get("/account").session(session)).andExpect(status().is3xxRedirection());
        assertNull(session.getAttribute(WebSessionSupport.USER_ID_SESSION_KEY));
    }

    @Test
    void rawApiCannotBypassWebGuards() throws Exception {
        mvc.perform(get("/api/v1/users")).andExpect(status().isForbidden());
        verifyNoInteractions(users, userService);
    }

    @Test
    void logoutRequiresCsrfAndInvalidatesSession() throws Exception {
        when(users.findById(1L)).thenReturn(Optional.of(user(1L, "narin", Role.USER)));
        MockHttpSession session = authenticatedSession(1L);
        mvc.perform(post("/logout").session(session).param("_csrf", "test-csrf"))
                .andExpect(redirectedUrl("/login?loggedOut"));
        assertTrue(session.isInvalid());
    }

    @Test
    void encodedOrProtocolRelativeRedirectsAreNotAccepted() {
        assertEquals("/account", WebSessionSupport.safeNext("//example.invalid"));
        assertEquals("/account", WebSessionSupport.safeNext("/%2f%2fexample.invalid"));
        assertEquals("/account", WebSessionSupport.safeNext("/\\example.invalid"));
        assertEquals("/bookings/new", WebSessionSupport.safeNext("/bookings/new"));
    }

    @Test
    void malformedDateReturnsFriendlyBadRequestPage() throws Exception {
        when(users.findById(1L)).thenReturn(Optional.of(user(1L, "narin", Role.USER)));
        mvc.perform(get("/bookings/date").session(authenticatedSession(1L)).param("date", "not-a-date"))
                .andExpect(status().isBadRequest()).andExpect(view().name("common/error"));
    }

    @Test
    void unexpectedFailureDoesNotExposeExceptionDetails() throws Exception {
        when(users.findById(1L)).thenReturn(Optional.of(user(1L, "narin", Role.USER)));
        mvc.perform(get("/bookings/unexpected").session(authenticatedSession(1L)))
                .andExpect(status().isInternalServerError()).andExpect(view().name("common/error"))
                .andExpect(model().attribute("message", "ลองอีกครั้งในภายหลัง หรือกลับไปเลือกพื้นที่ใหม่"));
    }

    private MockHttpSession anonymousSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSessionSupport.CSRF_SESSION_KEY, "test-csrf");
        return session;
    }

    private MockHttpSession authenticatedSession(Long userId) {
        MockHttpSession session = anonymousSession();
        session.setAttribute(WebSessionSupport.USER_ID_SESSION_KEY, userId);
        return session;
    }

    private User user(Long id, String username, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setRole(role);
        return user;
    }

    @Controller
    static class ProtectedPages {
        @GetMapping("/bookings") String bookings() { return "booking/list"; }
        @GetMapping("/account") String account() { return "account/index"; }
        @GetMapping("/admin") String admin() { return "admin/overview"; }
        @GetMapping("/admin/users") String users() { return "admin/users"; }
        @GetMapping("/api/v1/users") String api() { return "account/index"; }
        @GetMapping("/bookings/date") String date(@RequestParam("date")
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) { return "booking/list"; }
        @GetMapping("/bookings/unexpected") String unexpected() {
            throw new IllegalStateException("Internal diagnostic details must not appear in the page");
        }
    }
}
