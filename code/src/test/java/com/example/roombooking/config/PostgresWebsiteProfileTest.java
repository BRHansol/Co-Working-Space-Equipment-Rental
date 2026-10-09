package com.example.roombooking.config;

import com.example.roombooking.controller.web.AuthViewController;
import com.example.roombooking.controller.web.CatalogViewController;
import com.example.roombooking.controller.web.support.WebAccessInterceptor;
import com.example.roombooking.repository.EquipmentRepository;
import com.example.roombooking.repository.MeetingRoomRepository;
import com.example.roombooking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** Check website/profile isolation without connecting tests to a real database. */
@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:postgres-website-profile;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PostgresWebsiteProfileTest {
    @Autowired ApplicationContext context;
    @Autowired UserRepository users;
    @Autowired MeetingRoomRepository rooms;
    @Autowired EquipmentRepository equipment;

    @Test
    void productionWebsiteKeepsWebGuardsWithoutDemoAccounts() {
        assertTrue(Arrays.asList(context.getEnvironment().getActiveProfiles())
                .containsAll(Arrays.asList("prod", "web", "postgres")));
        assertEquals("0.0.0.0", context.getEnvironment().getProperty("server.address"));
        assertEquals(Boolean.TRUE, context.getEnvironment()
                .getProperty("server.servlet.session.cookie.secure", Boolean.class));
        assertNotNull(context.getBean(AuthViewController.class));
        assertNotNull(context.getBean(CatalogViewController.class));
        assertNotNull(context.getBean(WebAssetsConfig.class));
        assertNotNull(context.getBean(WebAccessInterceptor.class));
        assertEquals(1, context.getBeansOfType(WebSecurityConfig.class).size());
        assertEquals(1, context.getBeansOfType(PasswordEncoder.class).size());
        assertTrue(context.getBeansOfType(SecurityConfig.class).isEmpty());
        assertTrue(context.getBeansOfType(UserIdHeaderInterceptor.class).isEmpty());
        assertEquals(0, users.count());
        assertEquals(0, rooms.count());
        assertEquals(0, equipment.count());
    }
}
