package com.bank.customers.domain.port.in;

import com.bank.customers.domain.model.Customer;

import java.util.UUID;

public interface UpdateCustomerUseCase {
    Customer execute(UUID id, String name, String email);
}
