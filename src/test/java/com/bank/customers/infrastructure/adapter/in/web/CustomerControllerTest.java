package com.bank.customers.infrastructure.adapter.in.web;

import com.bank.customers.domain.exception.CustomerNotFoundException;
import com.bank.customers.domain.model.Customer;
import com.bank.customers.domain.port.in.CreateCustomerUseCase;
import com.bank.customers.domain.port.in.DeleteCustomerUseCase;
import com.bank.customers.domain.port.in.GetCustomersUseCase;
import com.bank.customers.domain.port.in.UpdateCustomerUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CustomerController Integration Tests")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateCustomerUseCase createCustomerUseCase;

    @MockBean
    private GetCustomersUseCase getCustomersUseCase;

    @MockBean
    private UpdateCustomerUseCase updateCustomerUseCase;

    @MockBean
    private DeleteCustomerUseCase deleteCustomerUseCase;

    @MockBean
    private com.bank.customers.infrastructure.security.JwtService jwtService;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/customers - should return 201 when valid request")
    void shouldCreateCustomer() throws Exception {
        var customer = Customer.create("Juan Perez", "juan@email.com");
        when(createCustomerUseCase.execute(anyString(), anyString())).thenReturn(customer);

        var body = Map.of("name", "Juan Perez", "email", "juan@email.com");

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Juan Perez"))
            .andExpect(jsonPath("$.email").value("juan@email.com"))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/customers - should return 400 when invalid email")
    void shouldReturn400OnInvalidEmail() throws Exception {
        var body = Map.of("name", "Juan", "email", "not-an-email");

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Error"))
            .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /api/customers - should return 400 when name is blank")
    void shouldReturn400OnBlankName() throws Exception {
        var body = Map.of("name", "", "email", "juan@email.com");

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @DisplayName("GET /api/customers - should return list of customers")
    void shouldReturnAllCustomers() throws Exception {
        var customers = List.of(
            Customer.create("Juan", "juan@email.com"),
            Customer.create("Maria", "maria@email.com")
        );
        when(getCustomersUseCase.execute()).thenReturn(customers);

        mockMvc.perform(get("/api/customers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("PUT /api/customers/{id} - should return 200 when valid update")
    void shouldUpdateCustomer() throws Exception {
        var id = UUID.randomUUID();
        var updated = Customer.reconstitute(
            com.bank.customers.domain.model.CustomerId.of(id),
            "Juan Updated", "juan.updated@email.com", java.time.Instant.now());
        when(updateCustomerUseCase.execute(eq(id), anyString(), anyString())).thenReturn(updated);

        var body = Map.of("name", "Juan Updated", "email", "juan.updated@email.com");

        mockMvc.perform(put("/api/customers/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Juan Updated"))
            .andExpect(jsonPath("$.email").value("juan.updated@email.com"));
    }

    @Test
    @DisplayName("PUT /api/customers/{id} - should return 404 when customer not found")
    void shouldReturn404OnUpdateMissingCustomer() throws Exception {
        var id = UUID.randomUUID();
        when(updateCustomerUseCase.execute(eq(id), anyString(), anyString()))
            .thenThrow(new CustomerNotFoundException(id));

        var body = Map.of("name", "Juan", "email", "juan@email.com");

        mockMvc.perform(put("/api/customers/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Customer Not Found"));
    }

    @Test
    @DisplayName("DELETE /api/customers/{id} - should return 204 on success")
    void shouldDeleteCustomer() throws Exception {
        var id = UUID.randomUUID();
        doNothing().when(deleteCustomerUseCase).execute(id);

        mockMvc.perform(delete("/api/customers/{id}", id))
            .andExpect(status().isNoContent());

        verify(deleteCustomerUseCase).execute(id);
    }

    @Test
    @DisplayName("DELETE /api/customers/{id} - should return 404 when customer not found")
    void shouldReturn404OnDeleteMissingCustomer() throws Exception {
        var id = UUID.randomUUID();
        doThrow(new CustomerNotFoundException(id)).when(deleteCustomerUseCase).execute(id);

        mockMvc.perform(delete("/api/customers/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Customer Not Found"));
    }
}
