package com.trt.contentengagement.identity.infrastructure.security;

import com.trt.contentengagement.identity.application.PasswordHashService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BcryptPasswordHashService implements PasswordHashService {
    private final PasswordEncoder passwordEncoder;

    BcryptPasswordHashService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}

