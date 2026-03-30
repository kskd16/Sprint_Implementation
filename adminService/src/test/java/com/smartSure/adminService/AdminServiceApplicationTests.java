package com.smartSure.adminService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — verifies the Spring application context loads without errors.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.cache.type=none",
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "management.tracing.enabled=false"
})
class AdminServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
