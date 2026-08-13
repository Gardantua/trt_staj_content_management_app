package com.trt.contentengagement.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.trt.contentengagement.identity.application.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "app.identity.initial-admin.enabled=true",
        "app.identity.initial-admin.email=admin@example.com",
        "app.identity.initial-admin.display-name=Initial Admin",
        "app.identity.initial-admin.password=unique-admin-password"
})
class InitialAdminBootstrapIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL_CONTAINER =
            new PostgreSQLContainer("postgres:17.5-alpine")
                    .withDatabaseName("content_engagement_test")
                    .withUsername("content_engagement")
                    .withPassword("test_password");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    AccountService accountService;

    @Test
    void createsOneHashedAdministratorAndDoesNotCreateASecondOne() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT role FROM identity_user_accounts WHERE normalized_email = 'admin@example.com'",
                String.class
        )).isEqualTo("ADMIN");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT password_hash FROM identity_user_accounts WHERE normalized_email = 'admin@example.com'",
                String.class
        )).startsWith("{bcrypt}").doesNotContain("unique-admin-password");

        assertThat(accountService.createInitialAdmin(
                "other-admin@example.com", "Other Admin", "another-admin-password"
        )).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity_user_accounts WHERE role = 'ADMIN'",
                Integer.class
        )).isEqualTo(1);
    }
}
