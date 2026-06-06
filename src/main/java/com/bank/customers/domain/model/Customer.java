package com.bank.customers.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class Customer {

    private final CustomerId id;
    private final String name;
    private final String email;
    private final Instant createdAt;

    private Customer(CustomerId id, String name, String email, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Customer create(String name, String email) {
        return new Customer(CustomerId.generate(), name, email, Instant.now());
    }

    public static Customer reconstitute(CustomerId id, String name, String email, Instant createdAt) {
        return new Customer(id, name, email, createdAt);
    }

    public Customer withDetails(String newName, String newEmail) {
        return new Customer(this.id, newName, newEmail, this.createdAt);
    }

    public CustomerId getId()      { return id; }
    public String getName()        { return name; }
    public String getEmail()       { return email; }
    public Instant getCreatedAt()  { return createdAt; }
}
