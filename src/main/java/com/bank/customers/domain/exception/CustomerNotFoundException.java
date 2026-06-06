package com.bank.customers.domain.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(UUID id) {
        super("Customer with id '%s' was not found".formatted(id));
    }
}
