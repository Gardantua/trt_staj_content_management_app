package com.trt.contentengagement.identity.application;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import com.trt.contentengagement.identity.domain.UserAccount;
import com.trt.contentengagement.identity.domain.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordHashService passwordHashService;
    private final Clock clock;

    public AccountService(
            UserAccountRepository userAccountRepository,
            PasswordHashService passwordHashService,
            Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHashService = passwordHashService;
        this.clock = clock;
    }

    @Transactional
    public AuthenticatedAccount register(String email, String displayName, String password) {
        String normalizedEmail = UserAccount.normalizeEmail(email);
        if (userAccountRepository.findByNormalizedEmail(normalizedEmail).isPresent()) {
            throw new AccountEmailAlreadyUsedException();
        }

        UserAccount userAccount = new UserAccount(
                UUID.randomUUID(),
                email,
                normalizedEmail,
                displayName,
                passwordHashService.hash(password),
                UserRole.USER,
                clock.instant()
        );
        return toAuthenticatedAccount(userAccountRepository.save(userAccount));
    }

    /** Creates the first administrator only while no ADMIN account exists. */
    @Transactional
    public boolean createInitialAdmin(String email, String displayName, String password) {
        if (userAccountRepository.existsByRole(UserRole.ADMIN)) {
            return false;
        }

        String normalizedEmail = UserAccount.normalizeEmail(email);
        if (userAccountRepository.findByNormalizedEmail(normalizedEmail).isPresent()) {
            throw new IllegalStateException(
                    "The initial administrator email already belongs to another account."
            );
        }

        UserAccount administrator = new UserAccount(
                UUID.randomUUID(),
                email,
                normalizedEmail,
                displayName,
                passwordHashService.hash(password),
                UserRole.ADMIN,
                clock.instant()
        );
        userAccountRepository.save(administrator);
        return true;
    }

    @Transactional(readOnly = true)
    public AuthenticatedAccount login(String email, String password) {
        String normalizedEmail = UserAccount.normalizeEmail(email);
        UserAccount userAccount = userAccountRepository.findByNormalizedEmail(normalizedEmail)
                .orElseThrow(InvalidLoginCredentialsException::new);
        if (!passwordHashService.matches(password, userAccount.passwordHash())) {
            throw new InvalidLoginCredentialsException();
        }
        return toAuthenticatedAccount(userAccount);
    }

    @Transactional(readOnly = true)
    public AuthenticatedAccount getAccount(UUID accountId) {
        return userAccountRepository.findById(accountId)
                .map(this::toAuthenticatedAccount)
                .orElseThrow(AccountNotFoundException::new);
    }

    private AuthenticatedAccount toAuthenticatedAccount(UserAccount userAccount) {
        return new AuthenticatedAccount(
                userAccount.id(),
                userAccount.email(),
                userAccount.displayName(),
                Set.of(userAccount.role())
        );
    }
}
