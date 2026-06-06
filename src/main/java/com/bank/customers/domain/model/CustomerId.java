package com.bank.customers.domain.model;

import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) throw new IllegalArgumentException("CustomerId cannot be null");
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    public static CustomerId of(UUID value) {
        return new CustomerId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
