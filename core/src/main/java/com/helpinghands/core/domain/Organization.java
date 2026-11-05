package com.helpinghands.core.domain;

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

    protected Organization() {
    }

    private Organization(String name, String email, String description) {
        this.name = name;
        this.email = email;
        this.description = description;
    }

    public static Organization register(String name, String email, String description) {
        return new Organization(name, email, description);
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
}
