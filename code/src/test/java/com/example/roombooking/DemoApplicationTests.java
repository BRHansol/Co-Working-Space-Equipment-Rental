package com.example.roombooking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:context-test;MODE=PostgreSQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
