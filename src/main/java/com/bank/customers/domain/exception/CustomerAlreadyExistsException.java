package com.bank.customers.domain.exception;

public class CustomerAlreadyExistsException extends RuntimeException {

    public CustomerAlreadyExistsException(String email) {
        super("Customer with email '%s' already exists".formatted(email));
    }
}
