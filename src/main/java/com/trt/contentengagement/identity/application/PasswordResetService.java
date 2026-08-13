package com.trt.contentengagement.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.identity.domain.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetTokenGenerator tokenGenerator;
    private final PasswordResetNotificationPort notificationPort;
    private final PasswordHashService passwordHashService;
    private final Clock clock;
    private final Duration tokenTtl;

    public PasswordResetService(
            UserAccountRepository userAccountRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordResetTokenGenerator tokenGenerator,
            PasswordResetNotificationPort notificationPort,
            PasswordHashService passwordHashService,
            Clock clock,
            @Value("${app.identity.password-reset.token-ttl}") Duration tokenTtl
    ) {
        this.userAccountRepository = userAccountRepository;
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.notificationPort = notificationPort;
        this.passwordHashService = passwordHashService;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
    }

    @Transactional
    public void requestReset(String email) {
        userAccountRepository.findByNormalizedEmail(UserAccount.normalizeEmail(email))
                .ifPresent(this::createAndSendToken);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        Instant consumedAt = clock.instant();
        UUID accountId = tokenRepository.consume(tokenGenerator.hash(rawToken), consumedAt)
                .orElseThrow(InvalidPasswordResetTokenException::new);
        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(InvalidPasswordResetTokenException::new);
        userAccountRepository.save(account.withPasswordHash(passwordHashService.hash(newPassword)));
    }

    private void createAndSendToken(UserAccount account) {
        String rawToken = tokenGenerator.generate();
        Instant createdAt = clock.instant();
        tokenRepository.replaceActiveToken(
                account.id(), tokenGenerator.hash(rawToken), createdAt.plus(tokenTtl), createdAt
        );
        notificationPort.sendPasswordResetLink(account.email(), rawToken);
    }
}
