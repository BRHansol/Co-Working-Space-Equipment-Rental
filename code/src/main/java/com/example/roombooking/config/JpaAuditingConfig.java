package com.example.roombooking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// ต้องเปิดไว้ ไม่งั้น @CreatedDate / @LastModifiedDate (เช่น changedAt ใน BookingStatusHistory) จะเป็น null เสมอ
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

}
