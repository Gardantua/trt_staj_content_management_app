package com.trt.contentengagement.identity.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.trt.contentengagement.identity.application.AccountEmailAlreadyUsedException;
import com.trt.contentengagement.identity.application.UserAccountRepository;
import com.trt.contentengagement.identity.domain.UserAccount;
import com.trt.contentengagement.identity.domain.UserRole;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class JpaUserAccountRepositoryAdapter implements UserAccountRepository {
    private final SpringDataUserAccountRepository repository;

    JpaUserAccountRepositoryAdapter(SpringDataUserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserAccount> findByNormalizedEmail(String normalizedEmail) {
        return repository.findByNormalizedEmail(normalizedEmail).map(JpaUserAccountEntity::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(UUID accountId) {
        return repository.findById(accountId).map(JpaUserAccountEntity::toDomain);
    }

    @Override
    public List<UserAccount> findAllById(Set<UUID> accountIds) {
        return repository.findAllById(accountIds).stream()
                .map(JpaUserAccountEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByRole(UserRole role) {
        return repository.existsByRole(role);
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        try {
            return repository.saveAndFlush(new JpaUserAccountEntity(userAccount)).toDomain();
        } catch (DataIntegrityViolationException conflict) {
            throw new AccountEmailAlreadyUsedException();
        }
    }
}
