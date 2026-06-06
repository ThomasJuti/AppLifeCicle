package com.bank.customers.application.dto;

import com.bank.customers.domain.model.Customer;
import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
    UUID id,
    String name,
    String email,
    Instant createdAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
            customer.getId().value(),
            customer.getName(),
            customer.getEmail(),
            customer.getCreatedAt()
        );
    }
}
