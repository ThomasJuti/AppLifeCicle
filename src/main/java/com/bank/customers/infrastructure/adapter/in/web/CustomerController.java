package com.bank.customers.infrastructure.adapter.in.web;

import com.bank.customers.application.dto.CreateCustomerRequest;
import com.bank.customers.application.dto.CustomerResponse;
import com.bank.customers.application.dto.UpdateCustomerRequest;
import com.bank.customers.domain.port.in.CreateCustomerUseCase;
import com.bank.customers.domain.port.in.DeleteCustomerUseCase;
import com.bank.customers.domain.port.in.GetCustomersUseCase;
import com.bank.customers.domain.port.in.UpdateCustomerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Customer registration and retrieval operations")
public class CustomerController {

    private final CreateCustomerUseCase createCustomerUseCase;
    private final GetCustomersUseCase getCustomersUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final DeleteCustomerUseCase deleteCustomerUseCase;

    public CustomerController(
        CreateCustomerUseCase createCustomerUseCase,
        GetCustomersUseCase getCustomersUseCase,
        UpdateCustomerUseCase updateCustomerUseCase,
        DeleteCustomerUseCase deleteCustomerUseCase
    ) {
        this.createCustomerUseCase = createCustomerUseCase;
        this.getCustomersUseCase = getCustomersUseCase;
        this.updateCustomerUseCase = updateCustomerUseCase;
        this.deleteCustomerUseCase = deleteCustomerUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new customer",
        description = "Registers a new customer. Email must be unique.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Customer created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Customer with email already exists")
        }
    )
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest request) {
        var customer = createCustomerUseCase.execute(request.name(), request.email());
        return CustomerResponse.from(customer);
    }

    @GetMapping
    @Operation(
        summary = "List all customers",
        description = "Returns all registered customers.",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of customers")
        }
    )
    public List<CustomerResponse> getAll() {
        return getCustomersUseCase.execute()
            .stream()
            .map(CustomerResponse::from)
            .toList();
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update an existing customer",
        description = "Updates name and email of a customer. Email must remain unique.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Customer updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "409", description = "Customer with email already exists")
        }
    )
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
        var customer = updateCustomerUseCase.execute(id, request.name(), request.email());
        return CustomerResponse.from(customer);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete a customer",
        description = "Removes a customer by id.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Customer deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
        }
    )
    public void delete(@PathVariable UUID id) {
        deleteCustomerUseCase.execute(id);
    }
}
