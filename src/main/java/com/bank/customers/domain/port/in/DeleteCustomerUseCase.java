package com.bank.customers.domain.port.in;

import java.util.UUID;

public interface DeleteCustomerUseCase {
    void execute(UUID id);
}
