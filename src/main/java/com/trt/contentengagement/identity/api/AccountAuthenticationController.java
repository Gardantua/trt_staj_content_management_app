package com.trt.contentengagement.identity.api;

import java.util.stream.Collectors;

import com.trt.contentengagement.identity.application.AccountService;
import com.trt.contentengagement.identity.application.AuthenticatedAccount;
import com.trt.contentengagement.identity.application.CurrentActorProvider;
import com.trt.contentengagement.identity.application.PasswordResetService;
import com.trt.contentengagement.identity.application.PasswordResetDeliveryException;
import com.trt.contentengagement.identity.infrastructure.security.AuthenticatedActorPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/auth")
public class AccountAuthenticationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountAuthenticationController.class);
    private final AccountService accountService;
    private final CurrentActorProvider currentActorProvider;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final PasswordResetService passwordResetService;

    public AccountAuthenticationController(
            AccountService accountService,
            CurrentActorProvider currentActorProvider,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            PasswordResetService passwordResetService
    ) {
        this.accountService = accountService;
        this.currentActorProvider = currentActorProvider;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountSessionResponse register(
            @Valid @RequestBody RegisterAccountRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthenticatedAccount account = accountService.register(
                requestBody.email(), requestBody.displayName(), requestBody.password()
        );
        establishSession(account, request, response);
        return AccountSessionResponse.from(account);
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

    @PostMapping("/login")
    public AccountSessionResponse login(
            @Valid @RequestBody LoginRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthenticatedAccount account = accountService.login(requestBody.email(), requestBody.password());
        establishSession(account, request, response);
        return AccountSessionResponse.from(account);
    }

    @GetMapping("/me")
    public AccountSessionResponse currentAccount() {
        return AccountSessionResponse.from(
                accountService.getAccount(currentActorProvider.getCurrentActor().actorId())
        );
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest requestBody) {
        try {
            passwordResetService.requestReset(requestBody.email());
        } catch (PasswordResetDeliveryException deliveryFailure) {
            LOGGER.warn("Password reset email delivery failed.");
        }
    }

    @PostMapping("/password-reset/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest requestBody) {
        passwordResetService.resetPassword(requestBody.token(), requestBody.newPassword());
    }

    private void establishSession(
            AuthenticatedAccount account,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var authorities = account.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toUnmodifiableSet());
        var principal = new AuthenticatedActorPrincipal(account.actorId(), account.roles());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, authorities
        );
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
    }
}
