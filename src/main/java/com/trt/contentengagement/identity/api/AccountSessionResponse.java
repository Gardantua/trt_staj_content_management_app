package com.trt.contentengagement.identity.api;

import java.util.List;

import com.trt.contentengagement.identity.application.AuthenticatedAccount;

public record AccountSessionResponse(
        String actorId,
        String email,
        String displayName,
        List<String> roles
) {
    static AccountSessionResponse from(AuthenticatedAccount account) {
        return new AccountSessionResponse(
                account.actorId().toString(),
                account.email(),
                account.displayName(),
                account.roles().stream().map(Enum::name).sorted().toList()
        );
    }
}
