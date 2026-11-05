package com.helpinghands.core.domain;

import com.helpinghands.core.exception.BusinessRuleException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String description;
    private String phone;
    private String website;
    private boolean verified;

    protected Organization() {
    }

    private Organization(String name, String email, String description, String phone, String website) {
        update(name, email, description, phone, website);
    }

    public static Organization register(String name, String email, String description) {
        return register(name, email, description, null, null);
    }

    public static Organization register(String name, String email, String description, String phone, String website) {
        return new Organization(name, email, description, phone, website);
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public void update(String name, String email, String description, String phone, String website) {
        this.name = name;
        this.email = normalizeEmail(email);
        this.description = description;
        this.phone = phone;
        this.website = website;
    }

    public void verify() {
        if (verified) {
            throw new BusinessRuleException("Organization is already verified");
        }
        verified = true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getDescription() {
        return description;
    }

    public String getPhone() {
        return phone;
    }

    public String getWebsite() {
        return website;
    }

    public boolean isVerified() {
        return verified;
    }
}
