package com.bank.customers.domain.service;

import com.bank.customers.domain.exception.CustomerAlreadyExistsException;
import com.bank.customers.domain.exception.CustomerNotFoundException;
import com.bank.customers.domain.model.Customer;
import com.bank.customers.domain.model.CustomerId;
import com.bank.customers.domain.port.in.CreateCustomerUseCase;
import com.bank.customers.domain.port.in.DeleteCustomerUseCase;
import com.bank.customers.domain.port.in.GetCustomersUseCase;
import com.bank.customers.domain.port.in.UpdateCustomerUseCase;
import com.bank.customers.domain.port.out.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService implements
    CreateCustomerUseCase,
    GetCustomersUseCase,
    UpdateCustomerUseCase,
    DeleteCustomerUseCase {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer execute(String name, String email) {
        if (customerRepository.existsByEmail(email)) {
            throw new CustomerAlreadyExistsException(email);
        }
        return customerRepository.save(Customer.create(name, email));
    }

    @Override
    public List<Customer> execute() {
        return customerRepository.findAll();
    }

    @Override
    public Customer execute(UUID id, String name, String email) {
        var customerId = CustomerId.of(id);
        var existing = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(id));

        if (customerRepository.existsByEmailAndIdNot(email, customerId)) {
            throw new CustomerAlreadyExistsException(email);
        }

        return customerRepository.save(existing.withDetails(name, email));
    }

    @Override
    public void execute(UUID id) {
        var customerId = CustomerId.of(id);
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException(id);
        }
        customerRepository.deleteById(customerId);
    }
}
