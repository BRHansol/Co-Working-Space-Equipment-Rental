package com.example.roombooking.config;

import com.example.roombooking.controller.web.AuthViewController;
import com.example.roombooking.controller.web.support.WebAccessInterceptor;
import com.example.roombooking.controller.web.support.WebSessionSupport;
import com.example.roombooking.controller.web.support.WebViewAdvice;
import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.repository.UserRepository;
import com.example.roombooking.service.UserService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tests the actual security filter chain without starting the application or connecting to a database. */
class SecurityProfileTest {
    @Test
    void webProfileSelectsOnlySessionWebsiteSecurityAndProvidesPasswordEncoder() {
        try (var context = securityContext("web")) {
            assertEquals(1, context.getBeansOfType(WebSecurityConfig.class).size());
            assertTrue(context.getBeansOfType(SecurityConfig.class).isEmpty());
            assertTrue(context.getBeansOfType(UserIdHeaderInterceptor.class).isEmpty());
            assertTrue(context.containsBean("webFilterChain"));
            assertFalse(context.containsBean("filterChain"));
            PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
            assertTrue(encoder.matches("test-password", encoder.encode("test-password")));
        }
    }

    @Test
    void nonWebProfileKeepsDevelopSecurityAndHeaderInterceptor() {
        try (var context = securityContext("api-test")) {
            assertTrue(context.getBeansOfType(WebSecurityConfig.class).isEmpty());
            assertEquals(1, context.getBeansOfType(SecurityConfig.class).size());
            assertEquals(1, context.getBeansOfType(UserIdHeaderInterceptor.class).size());
            assertFalse(context.containsBean("webFilterChain"));
            assertTrue(context.containsBean("filterChain"));
            assertNotNull(context.getBean(PasswordEncoder.class));
            assertNotNull(context.getBean("springSecurityFilterChain"));
        }
    }

    @Test
    void webLogoutWithoutCsrfReachesMvcGuardAndKeepsSession() throws Exception {
        try (var context = securityContext("web")) {
            MockHttpSession session = csrfSession();
            webMvc(context, mock(UserRepository.class)).perform(post("/logout").session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(view().name("common/error"));
            assertFalse(session.isInvalid());
        }
    }

    @Test
    void webLogoutWithCsrfUsesWebsiteControllerAndInvalidatesSession() throws Exception {
        try (var context = securityContext("web")) {
            UserRepository users = mock(UserRepository.class);
            User member = new User();
            member.setId(7L);
            member.setRole(Role.USER);
            when(users.findById(7L)).thenReturn(Optional.of(member));
            MockHttpSession session = csrfSession();
            session.setAttribute(WebSessionSupport.USER_ID_SESSION_KEY, 7L);
            webMvc(context, users).perform(post("/logout").session(session).param("_csrf", "test-token"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?loggedOut"));
            assertTrue(session.isInvalid());
        }
    }

    @Test
    void webApiCannotBypassWebsiteGuardsUsingDevelopHeader() throws Exception {
        try (var context = securityContext("web")) {
            webMvc(context, mock(UserRepository.class))
                    .perform(get("/api/v1/bookings").header("X-User-Id", "7"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void apiWriteWithoutHeaderRemainsUnauthorized() throws Exception {
        try (var context = securityContext("api-test")) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertFalse(context.getBean(UserIdHeaderInterceptor.class)
                    .preHandle(new MockHttpServletRequest("POST", "/api/v1/bookings"), response, new Object()));
            assertEquals(401, response.getStatus());
        }
    }

    @Test
    void apiWriteWithMalformedHeaderRemainsBadRequest() throws Exception {
        try (var context = securityContext("api-test")) {
            MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/bookings/1");
            request.addHeader("X-User-Id", "invalid");
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertFalse(context.getBean(UserIdHeaderInterceptor.class).preHandle(request, response, new Object()));
            assertEquals(400, response.getStatus());
        }
    }

    @Test
    void apiWriteWithNumericHeaderAndReadWithoutHeaderRemainAllowed() throws Exception {
        try (var context = securityContext("api-test")) {
            UserIdHeaderInterceptor interceptor = context.getBean(UserIdHeaderInterceptor.class);
            MockHttpServletRequest write = new MockHttpServletRequest("PATCH", "/api/v1/bookings/1/status");
            write.addHeader("X-User-Id", "7");
            assertTrue(interceptor.preHandle(write, new MockHttpServletResponse(), new Object()));
            assertTrue(interceptor.preHandle(new MockHttpServletRequest("GET", "/api/v1/bookings"),
                    new MockHttpServletResponse(), new Object()));
        }
    }

    private AnnotationConfigWebApplicationContext securityContext(String profile) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().setActiveProfiles(profile);
        context.register(SecurityConfig.class, WebSecurityConfig.class, UserIdHeaderInterceptor.class);
        context.refresh();
        return context;
    }

    private MockMvc webMvc(AnnotationConfigWebApplicationContext context, UserRepository users) {
        WebSessionSupport sessions = new WebSessionSupport(users);
        return MockMvcBuilders.standaloneSetup(new AuthViewController(users, mock(UserService.class), sessions),
                        new ApiProbe())
                .setControllerAdvice(new WebViewAdvice(sessions))
                .addInterceptors(new WebAccessInterceptor(sessions))
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    private MockHttpSession csrfSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(WebSessionSupport.CSRF_SESSION_KEY, "test-token");
        return session;
    }

    @RestController
    static class ApiProbe {
        @GetMapping("/api/v1/bookings")
        String bookings() { return "API should be blocked in web"; }
    }
}
