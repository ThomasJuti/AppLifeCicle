package com.bank.customers.infrastructure.adapter.out.persistence;

import com.bank.customers.domain.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CustomerPersistenceAdapter.class)
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@DisplayName("CustomerPersistenceAdapter Integration Tests (Testcontainers)")
class CustomerPersistenceAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CustomerPersistenceAdapter adapter;

    @Test
    @DisplayName("Should save and retrieve customer from PostgreSQL")
    void shouldSaveAndRetrieveCustomer() {
        var customer = Customer.create("Juan Perez", "juan@email.com");

        var saved = adapter.save(customer);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("juan@email.com");
    }

    @Test
    @DisplayName("Should detect existing email")
    void shouldDetectExistingEmail() {
        adapter.save(Customer.create("Juan", "juan@email.com"));

        assertThat(adapter.existsByEmail("juan@email.com")).isTrue();
        assertThat(adapter.existsByEmail("other@email.com")).isFalse();
    }

    @Test
    @DisplayName("Should return all saved customers")
    void shouldFindAllCustomers() {
        adapter.save(Customer.create("Juan", "juan@email.com"));
        adapter.save(Customer.create("Maria", "maria@email.com"));

        var all = adapter.findAll();

        assertThat(all).hasSize(2);
    }
}
