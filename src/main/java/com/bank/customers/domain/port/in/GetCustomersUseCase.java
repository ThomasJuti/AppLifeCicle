package com.bank.customers.domain.port.in;

import com.bank.customers.domain.model.Customer;
import java.util.List;

public interface GetCustomersUseCase {
    List<Customer> execute();
}
