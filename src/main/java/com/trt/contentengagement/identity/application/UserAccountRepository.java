package com.trt.contentengagement.identity.application;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.trt.contentengagement.identity.domain.UserAccount;
import com.trt.contentengagement.identity.domain.UserRole;

public interface UserAccountRepository {
    Optional<UserAccount> findByNormalizedEmail(String normalizedEmail);

    Optional<UserAccount> findById(UUID accountId);

    List<UserAccount> findAllById(Set<UUID> accountIds);

    boolean existsByRole(UserRole role);

    UserAccount save(UserAccount userAccount);
}
