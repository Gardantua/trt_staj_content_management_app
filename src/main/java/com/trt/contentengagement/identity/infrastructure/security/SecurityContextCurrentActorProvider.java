package com.trt.contentengagement.identity.infrastructure.security;

import com.trt.contentengagement.identity.application.CurrentActor;
import com.trt.contentengagement.identity.application.CurrentActorProvider;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentActorProvider implements CurrentActorProvider {

    @Override
    public CurrentActor getCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedActorPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "No verified actor is available for the current request."
            );
        }

        return new CurrentActor(principal.actorId(), principal.roles());
    }
}
