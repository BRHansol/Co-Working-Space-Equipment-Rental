package com.example.roombooking.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor u0e17u0e35u0e48u0e15u0e23u0e27u0e08u0e2au0e2du0e1au0e27u0e48u0e32 request u0e21u0e35 X-User-Id header u0e17u0e35u0e48u0e16u0e39u0e01u0e15u0e49u0e2du0e07
 * u0e2au0e33u0e2bu0e23u0e31u0e1a endpoint u0e17u0e35u0e48u0e15u0e49u0e2du0e07u0e01u0e32u0e23 identity u0e02u0e2du0e07u0e1cu0e39u0e49u0e08u0e2du0e07 (POST/PUT/PATCH bookings)
 *
 * <p>u0e40u0e21u0e37u0e48u0e2du0e23u0e30u0e1au0e1a authentication u0e08u0e23u0e34u0e07u0e1eu0e23u0e49u0e2du0e21 u0e43u0e2bu0e49u0e41u0e17u0e19u0e17u0e35u0e48 interceptor u0e19u0e35u0e49u0e14u0e49u0e27u0e22
 * Spring Security filter u0e17u0e35u0e48u0e14u0e36u0e07 user ID u0e08u0e32u0e01 Authentication context u0e41u0e17u0e19
 */
@Component
@Profile("!web")
public class UserIdHeaderInterceptor implements HandlerInterceptor {

    public static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String method = request.getMethod();

        // u0e40u0e09u0e1eu0e32u0e30 method u0e17u0e35u0e48u0e40u0e1bu0e25u0e35u0e48u0e22u0e19u0e41u0e1bu0e25u0e07u0e02u0e49u0e2du0e21u0e39u0e25u0e15u0e49u0e2du0e07u0e21u0e35 X-User-Id
        if ("POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)) {

            String userIdHeader = request.getHeader(USER_ID_HEADER);

            if (userIdHeader == null || userIdHeader.isBlank()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"X-User-Id header is required.\"}"
                );
                return false;
            }

            try {
                Long.parseLong(userIdHeader.trim());
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"error\":\"Bad Request\",\"message\":\"X-User-Id header must be a valid numeric ID.\"}"
                );
                return false;
            }
        }

        return true;
    }
}
