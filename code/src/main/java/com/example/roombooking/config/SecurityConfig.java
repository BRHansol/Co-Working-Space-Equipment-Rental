package com.example.roombooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    private final UserIdHeaderInterceptor userIdHeaderInterceptor;

    public SecurityConfig(UserIdHeaderInterceptor userIdHeaderInterceptor) {
        this.userIdHeaderInterceptor = userIdHeaderInterceptor;
    }

    /**
     * อนุญาต request ทั้งหมด — ระบบยังใช้ X-User-Id header ผ่าน interceptor แทน
     * เมื่อ implement authentication จริงให้เปลี่ยน authorizeHttpRequests ที่นี่
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /** BCrypt cost factor 12 — ปลอดภัยเพียงพอและรวดเร็วพอสำหรับ production */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userIdHeaderInterceptor)
                .addPathPatterns(
                        "/api/v1/bookings",
                        "/api/v1/bookings/**"
                );
    }
}
