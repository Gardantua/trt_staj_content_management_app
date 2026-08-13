package com.trt.contentengagement.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.trt.contentengagement.identity.domain.UserAccount;
import com.trt.contentengagement.identity.domain.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "identity_user_accounts")
class JpaUserAccountEntity {
    @Id
    UUID id;
    @Column(nullable = false, length = 254)
    String email;
    @Column(name = "normalized_email", nullable = false, length = 254)
    String normalizedEmail;
    @Column(name = "display_name", nullable = false, length = 80)
    String displayName;
    @Column(name = "password_hash", nullable = false, length = 255)
    String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    UserRole role;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected JpaUserAccountEntity() {
    }

    JpaUserAccountEntity(UserAccount userAccount) {
        id = userAccount.id();
        email = userAccount.email();
        normalizedEmail = userAccount.normalizedEmail();
        displayName = userAccount.displayName();
        passwordHash = userAccount.passwordHash();
        role = userAccount.role();
        createdAt = userAccount.createdAt();
    }

    UserAccount toDomain() {
        return new UserAccount(id, email, normalizedEmail, displayName, passwordHash, role, createdAt);
    }
}

