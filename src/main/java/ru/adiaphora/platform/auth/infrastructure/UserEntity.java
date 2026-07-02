package ru.adiaphora.platform.auth.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.adiaphora.platform.auth.domain.User;
import ru.adiaphora.platform.auth.domain.UserRole;
import ru.adiaphora.platform.auth.domain.UserStatus;
import ru.adiaphora.platform.common.persistence.BaseEntity;

import java.util.UUID;

/**
 * JPA persistence view of the {@link User} aggregate. Kept in infrastructure so the domain stays free
 * of persistence annotations. Mapping to/from the domain type is done here to keep it in one place.
 */
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion;

    protected UserEntity() {
        // for JPA
    }

    static UserEntity fromDomain(User user) {
        UserEntity entity = new UserEntity();
        entity.applyFrom(user);
        return entity;
    }

    void applyFrom(User user) {
        this.id = user.id();
        this.email = user.email();
        this.passwordHash = user.passwordHash();
        this.status = user.status();
        this.role = user.role();
        this.tokenVersion = user.tokenVersion();
    }

    User toDomain() {
        return User.rehydrate(id, email, passwordHash, status, role, tokenVersion);
    }

    UUID getId() {
        return id;
    }

    String getEmail() {
        return email;
    }

    UserRole getRole() {
        return role;
    }
}
