package com.helpinghands.core.domain;

import com.helpinghands.core.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean active = true;

    @Column(name = "organization_id")
    private Long organizationId;

    private Instant createdAt = Instant.now();

    protected User() {
    }

    private User(String email, String passwordHash, Role role) {
        this.email = Organization.normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public static User register(String email, String passwordHash, Role role) {
        return new User(email, passwordHash, role);
    }

    public void assignOrganization(Long organizationId) {
        if (role != Role.ORGANIZATION) {
            throw new BusinessRuleException("Only organization users can own an organization");
        }
        if (this.organizationId != null) {
            throw new BusinessRuleException("User already owns an organization");
        }
        this.organizationId = organizationId;
    }

    public boolean ownsOrganization(Long organizationId) {
        return active && role == Role.ORGANIZATION && this.organizationId != null
                && this.organizationId.equals(organizationId);
    }

    public void deactivate() {
        active = false;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
