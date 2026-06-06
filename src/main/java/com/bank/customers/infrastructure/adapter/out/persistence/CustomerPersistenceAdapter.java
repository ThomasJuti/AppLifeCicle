package com.bank.customers.infrastructure.adapter.out.persistence;

import com.bank.customers.domain.model.Customer;
import com.bank.customers.domain.model.CustomerId;
import com.bank.customers.domain.port.out.CustomerRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CustomerPersistenceAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpaRepository;

    public CustomerPersistenceAdapter(CustomerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Customer save(Customer customer) {
        var entity = toEntity(customer);
        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, CustomerId id) {
        return jpaRepository.existsByEmailAndIdNot(email, id.value());
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Customer> findAll() {
        return jpaRepository.findAll()
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public void deleteById(CustomerId id) {
        jpaRepository.deleteById(id.value());
    }

    private CustomerEntity toEntity(Customer customer) {
        return new CustomerEntity(
            customer.getId().value(),
            customer.getName(),
            customer.getEmail(),
            customer.getCreatedAt()
        );
    }

    private Customer toDomain(CustomerEntity entity) {
        return Customer.reconstitute(
            CustomerId.of(entity.getId()),
            entity.getName(),
            entity.getEmail(),
            entity.getCreatedAt()
        );
    }
}
