package com.example.roombooking.controller.web.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import org.springframework.web.util.UrlPathHelper;

@Component
@Profile("web")
public class WebAccessInterceptor implements HandlerInterceptor {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private final WebSessionSupport sessions;

    public WebAccessInterceptor(WebSessionSupport sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        if (path.equals("/api") || path.startsWith("/api/")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Website access uses the authenticated web pages.");
            return false;
        }
        if (!SAFE_METHODS.contains(request.getMethod()) && !sessions.hasValidCsrfToken(request)) {
            throw new WebAccessDeniedException("แบบฟอร์มหมดอายุ เปิดหน้าอีกครั้งแล้วลองส่งใหม่");
        }
        if (path.equals("/admin/users") || path.startsWith("/admin/users/")) {
            sessions.requireAdmin(request);
        } else if (path.equals("/admin") || path.startsWith("/admin/")) {
            sessions.requireManager(request);
        } else if (path.equals("/account") || path.startsWith("/account/")
                || path.equals("/bookings") || path.startsWith("/bookings/")) {
            sessions.requireUser(request);
        }
        return true;
    }
}
