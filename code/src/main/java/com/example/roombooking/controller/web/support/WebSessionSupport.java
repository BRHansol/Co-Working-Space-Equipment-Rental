package com.example.roombooking.controller.web.support;

import com.example.roombooking.domain.entity.User;
import com.example.roombooking.domain.enums.Role;
import com.example.roombooking.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Component
@Profile("web")
public class WebSessionSupport {
    public static final String USER_ID_SESSION_KEY = "authenticatedUserId";
    public static final String CSRF_SESSION_KEY = "webCsrfToken";
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public WebSessionSupport(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(USER_ID_SESSION_KEY) instanceof Long userId)) {
            return null;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getActive()) || user.getRole() == null) {
            session.removeAttribute(USER_ID_SESSION_KEY);
            return null;
        }
        return user;
    }

    public User requireUser(HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (user == null) {
            throw new WebLoginRequiredException();
        }
        return user;
    }

    public User requireManager(HttpServletRequest request) {
        User user = requireUser(request);
        if (!isManager(user)) {
            throw new WebAccessDeniedException("หน้านี้สำหรับเจ้าหน้าที่หรือผู้ดูแลระบบ");
        }
        return user;
    }

    public User requireAdmin(HttpServletRequest request) {
        User user = requireUser(request);
        if (user.getRole() != Role.ADMIN) {
            throw new WebAccessDeniedException("การจัดการผู้ใช้สำหรับผู้ดูแลระบบเท่านั้น");
        }
        return user;
    }

    public boolean isManager(User user) {
        return user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF);
    }

    public String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("รหัสผ่านต้องมีความยาวไม่เกิน 72 ไบต์");
        }
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return rawPassword != null && encodedPassword != null
                && rawPassword.getBytes(StandardCharsets.UTF_8).length <= 72
                && encodedPassword.startsWith("$2")
                && passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void login(HttpServletRequest request, User user) {
        request.getSession(true);
        request.changeSessionId();
        HttpSession session = request.getSession();
        session.setAttribute(USER_ID_SESSION_KEY, user.getId());
        session.setAttribute(CSRF_SESSION_KEY, UUID.randomUUID().toString());
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public String csrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        if (!(session.getAttribute(CSRF_SESSION_KEY) instanceof String)) {
            session.setAttribute(CSRF_SESSION_KEY, UUID.randomUUID().toString());
        }
        return (String) session.getAttribute(CSRF_SESSION_KEY);
    }

    public boolean hasValidCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String submitted = request.getParameter("_csrf");
        if (session == null || submitted == null
                || !(session.getAttribute(CSRF_SESSION_KEY) instanceof String expected)) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                submitted.getBytes(StandardCharsets.UTF_8));
    }

    public static String safeNext(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return "/account";
        }
        try {
            URI uri = URI.create(candidate);
            String path = uri.getPath();
            if (!uri.isAbsolute() && uri.getRawAuthority() == null && uri.getRawFragment() == null
                    && path != null && path.startsWith("/") && !path.startsWith("//")
                    && !path.contains("\\") && !path.contains("\r") && !path.contains("\n")
                    && !path.equals("/login") && !path.equals("/register") && !path.equals("/logout")) {
                return candidate;
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid redirects return to the account page.
        }
        return "/account";
    }
}
