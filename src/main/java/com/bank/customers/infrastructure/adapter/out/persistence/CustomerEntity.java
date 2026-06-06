package com.bank.customers.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customers_email", columnList = "email", unique = true)
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
