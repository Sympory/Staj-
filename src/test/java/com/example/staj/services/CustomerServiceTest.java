package com.example.staj.services;

import com.example.staj.entity.Customer;
import com.example.staj.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock CustomerRepository customerRepo;
    @InjectMocks CustomerService customerService;

    private Customer makeCustomer(Long id, String name, String email) {
        Customer c = new Customer();
        c.setName(name);
        c.setEmail(email);
        // ID setter yoksa testte ReflectionTestUtils.setField(c,"id",id); kullan
        org.springframework.test.util.ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    void list_returns_all_customers() {
        when(customerRepo.findAll()).thenReturn(List.of(makeCustomer(1L,"Ali","ali@test.com")));

        List<Customer> customers = customerService.list();

        assertThat(customers).hasSize(1);
        assertThat(customers.get(0).getName()).isEqualTo("Ali");
        verify(customerRepo).findAll();
    }

    @Test
    void create_saves_customer() {
        Customer input = makeCustomer(null,"Ayşe","ayse@test.com");
        Customer saved = makeCustomer(5L,"Ayşe","ayse@test.com");
        when(customerRepo.save(input)).thenReturn(saved);

        Customer result = customerService.create(input);

        assertThat(result.getName()).isEqualTo("Ayşe");
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(result,"id")).isEqualTo(5L);
        verify(customerRepo).save(input);
    }

    @Test
    void update_existing_customer_sets_fields_and_saves() {
        Customer existing = makeCustomer(10L,"Ali","old@test.com");
        when(customerRepo.findById(10L)).thenReturn(Optional.of(existing));
        when(customerRepo.save(existing)).thenAnswer(inv->inv.getArgument(0));

        Customer updated = customerService.update(10L,"Veli","veli@test.com");

        assertThat(updated.getName()).isEqualTo("Veli");
        assertThat(updated.getEmail()).isEqualTo("veli@test.com");
        verify(customerRepo).save(existing);
    }

    @Test
    void update_nonexistent_customer_throws() {
        when(customerRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            customerService.update(99L,"X","x@test.com")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Geçersiz müşteri ID");
    }

    @Test
    void get_existing_returns_customer() {
        Customer c = makeCustomer(11L,"Ali","ali@test.com");
        when(customerRepo.findById(11L)).thenReturn(Optional.of(c));

        Customer found = customerService.get(11L);

        assertThat(found.getName()).isEqualTo("Ali");
        verify(customerRepo).findById(11L);
    }

    @Test
    void get_nonexistent_throws() {
        when(customerRepo.findById(88L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            customerService.get(88L)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Müşteri bulunamadı");
    }

    @Test
    void delete_existing_calls_deleteById() {
        when(customerRepo.existsById(7L)).thenReturn(true);

        customerService.delete(7L);

        verify(customerRepo).deleteById(7L);
    }

    @Test
    void delete_nonexistent_throws() {
        when(customerRepo.existsById(77L)).thenReturn(false);

        assertThatThrownBy(() ->
            customerService.delete(77L)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Müşteri bulunamadı");
    }
}
