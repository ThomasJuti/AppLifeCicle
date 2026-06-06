package com.bank.customers.domain.port.in;

import com.bank.customers.domain.model.Customer;

public interface CreateCustomerUseCase {
    Customer execute(String name, String email);
}
