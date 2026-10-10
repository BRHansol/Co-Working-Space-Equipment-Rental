package com.example.roombooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// ยังไม่ประกาศ security scheme (เช่น JWT) เพราะระบบยังไม่มี login จริง
// ตอนนี้ระบุผู้ใช้ผ่าน header X-User-Id ซึ่ง springdoc แสดงเป็น parameter ของแต่ละ endpoint ให้อยู่แล้ว
// ถ้า SecurityConfig ทำ login เสร็จค่อยเพิ่ม SecurityScheme ให้ตรงกับของจริง
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Room Booking API").version("1.0.0")
                        .description("API Documentation for Corporate Room Booking System. "
                                + "ยังไม่มีระบบ login: endpoint ที่ต้องรู้ผู้ใช้ให้ส่ง header X-User-Id"));
    }
}
