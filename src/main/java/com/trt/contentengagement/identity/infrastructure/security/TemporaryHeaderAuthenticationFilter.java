package com.trt.contentengagement.identity.infrastructure.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.trt.contentengagement.identity.domain.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile({"local", "test"})
public class TemporaryHeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String ACTOR_ID_HEADER = "X-Test-Actor-Id";
    public static final String ACTOR_ROLES_HEADER = "X-Test-Actor-Roles";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TemporaryHeaderAuthenticationFilter.class);
    private static final String MISSING_HEADER_MESSAGE =
            "Both temporary actor headers must be supplied.";
    private static final String INVALID_ACTOR_ID_MESSAGE =
            "Temporary actor id is invalid.";
    private static final String INVALID_ROLES_MESSAGE =
            "Temporary actor roles are invalid.";

    private final SecurityErrorResponseWriter errorResponseWriter;

    public TemporaryHeaderAuthenticationFilter(SecurityErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestedActorId = request.getHeader(ACTOR_ID_HEADER);
        String requestedRoles = request.getHeader(ACTOR_ROLES_HEADER);

        if (requestedActorId == null && requestedRoles == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (requestedActorId == null || requestedRoles == null) {
            rejectAuthentication(response, MISSING_HEADER_MESSAGE);
            return;
        }

        UUID actorId;
        try {
            actorId = UUID.fromString(requestedActorId);
        } catch (IllegalArgumentException invalidActorIdException) {
            rejectAuthentication(response, INVALID_ACTOR_ID_MESSAGE);
            return;
        }

        Set<UserRole> roles;
        try {
            roles = parseRoles(requestedRoles);
        } catch (IllegalArgumentException invalidRolesException) {
            rejectAuthentication(response, INVALID_ROLES_MESSAGE);
            return;
        }

        AuthenticatedActorPrincipal principal =
                new AuthenticatedActorPrincipal(actorId, roles);
        var authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                authorities
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        filterChain.doFilter(request, response);
    }

    private Set<UserRole> parseRoles(String requestedRoles) {
        if (requestedRoles.isBlank()) {
            throw new IllegalArgumentException(INVALID_ROLES_MESSAGE);
        }

        EnumSet<UserRole> parsedRoles = EnumSet.noneOf(UserRole.class);
        Arrays.stream(requestedRoles.split(","))
                .map(String::trim)
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(UserRole::valueOf)
                .forEach(parsedRoles::add);

        if (parsedRoles.isEmpty()) {
            throw new IllegalArgumentException(INVALID_ROLES_MESSAGE);
        }

        return Set.copyOf(parsedRoles);
    }

    private void rejectAuthentication(HttpServletResponse response, String safeFailureReason)
            throws IOException {
        SecurityContextHolder.clearContext();
        LOGGER.warn("Rejected temporary authentication headers: {}", safeFailureReason);
        errorResponseWriter.write(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "AUTHENTICATION_INVALID",
                "The supplied test identity is invalid."
        );
    }
}
