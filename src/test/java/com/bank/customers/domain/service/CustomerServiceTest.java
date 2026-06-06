package com.bank.customers.domain.service;

import com.bank.customers.domain.exception.CustomerAlreadyExistsException;
import com.bank.customers.domain.exception.CustomerNotFoundException;
import com.bank.customers.domain.model.Customer;
import com.bank.customers.domain.model.CustomerId;
import com.bank.customers.domain.port.out.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        lenient().when(customerRepository.save(any(Customer.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should create customer successfully when email does not exist")
    void shouldCreateCustomerSuccessfully() {
        when(customerRepository.existsByEmail("juan@email.com")).thenReturn(false);

        var result = customerService.execute("Juan Perez", "juan@email.com");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Juan Perez");
        assertThat(result.getEmail()).isEqualTo("juan@email.com");
        assertThat(result.getId()).isNotNull();
        assertThat(result.getCreatedAt()).isNotNull();
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should throw CustomerAlreadyExistsException when email already exists")
    void shouldThrowWhenEmailAlreadyExists() {
        when(customerRepository.existsByEmail("juan@email.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.execute("Juan Perez", "juan@email.com"))
            .isInstanceOf(CustomerAlreadyExistsException.class)
            .hasMessageContaining("juan@email.com");

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return all customers")
    void shouldReturnAllCustomers() {
        var customer1 = Customer.create("Juan", "juan@email.com");
        var customer2 = Customer.create("Maria", "maria@email.com");
        when(customerRepository.findAll()).thenReturn(List.of(customer1, customer2));

        var result = customerService.execute();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Customer::getEmail)
            .containsExactlyInAnyOrder("juan@email.com", "maria@email.com");
    }

    @Test
    @DisplayName("Should update customer when it exists and email stays unique")
    void shouldUpdateCustomer() {
        var id = UUID.randomUUID();
        var existing = Customer.reconstitute(CustomerId.of(id), "Juan", "juan@email.com", Instant.now());
        when(customerRepository.findById(CustomerId.of(id))).thenReturn(Optional.of(existing));
        when(customerRepository.existsByEmailAndIdNot(eq("new@email.com"), eq(CustomerId.of(id))))
            .thenReturn(false);

        var result = customerService.execute(id, "Juan Updated", "new@email.com");

        assertThat(result.getName()).isEqualTo("Juan Updated");
        assertThat(result.getEmail()).isEqualTo("new@email.com");
        assertThat(result.getId().value()).isEqualTo(id);
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when updating a missing customer")
    void shouldThrowWhenUpdatingMissingCustomer() {
        var id = UUID.randomUUID();
        when(customerRepository.findById(CustomerId.of(id))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.execute(id, "X", "x@email.com"))
            .isInstanceOf(CustomerNotFoundException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CustomerAlreadyExistsException when new email belongs to another customer")
    void shouldThrowWhenUpdatingToDuplicateEmail() {
        var id = UUID.randomUUID();
        var existing = Customer.reconstitute(CustomerId.of(id), "Juan", "juan@email.com", Instant.now());
        when(customerRepository.findById(CustomerId.of(id))).thenReturn(Optional.of(existing));
        when(customerRepository.existsByEmailAndIdNot(eq("taken@email.com"), eq(CustomerId.of(id))))
            .thenReturn(true);

        assertThatThrownBy(() -> customerService.execute(id, "Juan", "taken@email.com"))
            .isInstanceOf(CustomerAlreadyExistsException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete customer when it exists")
    void shouldDeleteCustomer() {
        var id = UUID.randomUUID();
        var existing = Customer.reconstitute(CustomerId.of(id), "Juan", "juan@email.com", Instant.now());
        when(customerRepository.findById(CustomerId.of(id))).thenReturn(Optional.of(existing));

        customerService.execute(id);

        verify(customerRepository, times(1)).deleteById(CustomerId.of(id));
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when deleting a missing customer")
    void shouldThrowWhenDeletingMissingCustomer() {
        var id = UUID.randomUUID();
        when(customerRepository.findById(CustomerId.of(id))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.execute(id))
            .isInstanceOf(CustomerNotFoundException.class);

        verify(customerRepository, never()).deleteById(any());
    }
}
