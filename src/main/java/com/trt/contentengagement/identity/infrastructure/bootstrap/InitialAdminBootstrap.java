package com.trt.contentengagement.identity.infrastructure.bootstrap;

import com.trt.contentengagement.identity.application.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Creates the first production administrator from deployment secrets.
 * The switch must be disabled and the password removed after the first successful boot.
 */
@Component
@ConditionalOnProperty(
        name = "app.identity.initial-admin.enabled",
        havingValue = "true"
)
public class InitialAdminBootstrap implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitialAdminBootstrap.class);
    private static final int MINIMUM_BOOTSTRAP_PASSWORD_LENGTH = 12;

    private final AccountService accountService;
    private final String email;
    private final String displayName;
    private final String password;

    public InitialAdminBootstrap(
            AccountService accountService,
            @Value("${app.identity.initial-admin.email:}") String email,
            @Value("${app.identity.initial-admin.display-name:}") String displayName,
            @Value("${app.identity.initial-admin.password:}") String password
    ) {
        this.accountService = accountService;
        this.email = email;
        this.displayName = displayName;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        validateSecrets();
        boolean created = accountService.createInitialAdmin(email, displayName, password);
        if (created) {
            LOGGER.info(
                    "Initial administrator created. Disable bootstrap and remove its secrets."
            );
        } else {
            LOGGER.info("Initial administrator bootstrap skipped because an ADMIN already exists.");
        }
    }

    private void validateSecrets() {
        if (email.isBlank() || displayName.isBlank()) {
            throw new IllegalStateException(
                    "Initial administrator email and display name must be configured."
            );
        }
        if (password.length() < MINIMUM_BOOTSTRAP_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "Initial administrator password must contain at least 12 characters."
            );
        }
    }
}
