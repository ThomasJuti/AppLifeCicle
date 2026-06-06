package com.bank.customers.domain.port.out;

import com.bank.customers.domain.model.Customer;
import com.bank.customers.domain.model.CustomerId;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    Customer save(Customer customer);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, CustomerId id);
    Optional<Customer> findById(CustomerId id);
    List<Customer> findAll();
    void deleteById(CustomerId id);
}
