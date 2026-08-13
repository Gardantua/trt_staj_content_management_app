package com.trt.contentengagement.identity.application;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDisplayNameDirectory {
    private final UserAccountRepository userAccountRepository;

    public AccountDisplayNameDirectory(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> findDisplayNames(Set<UUID> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        return userAccountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toUnmodifiableMap(
                        account -> account.id(),
                        account -> account.displayName()
                ));
    }
}
