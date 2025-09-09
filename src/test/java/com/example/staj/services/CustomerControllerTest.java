package com.example.staj.services;  // services paketinde kalabilir

import com.example.staj.controller.CustomerController; // controller'ı import et
import com.example.staj.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring context açmadan (standaloneSetup) CustomerController testi.
 * Dosya services paketinde dursa da problemsiz çalışır.
 */
@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private MockMvc mvc;

    // Controller bağımlılıkları mock (services paketinde oldukları için erişim doğal)
    @Mock CustomerService customerService;
    @Mock CarService carService;
    @Mock PolicyService policyService;

    // Test edeceğimiz controller
    @InjectMocks CustomerController controller;

    @BeforeEach
    void setup() {
        // @Valid çalışsın diye JSR-303 validator ekleyelim
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    void list_customers_returns_view_and_model() throws Exception {
        Customer ali = new Customer();
        ali.setId(1L);
        ali.setName("Ali");
        ali.setEmail("ali@test.com");

        when(customerService.list()).thenReturn(List.of(ali));

        mvc.perform(get("/customers"))
           .andExpect(status().isOk())
           .andExpect(view().name("list-customers"))
           .andExpect(model().attributeExists("customers"))
           .andExpect(model().attribute("customers", hasSize(1)));

        verify(customerService).list();
    }

    @Test
    void create_customer_valid_redirects_with_flash() throws Exception {
        // controller create(...) dönüşünü kullanmıyor; stub şart değil
        mvc.perform(post("/customers")
                .param("name", "Ayse")
                .param("email", "ayse@test.com"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/customers"))
           .andExpect(flash().attribute("msg", containsString("Müşteri eklendi")));

        verify(customerService).create(any(Customer.class));
    }

    @Test
    void create_customer_invalid_returns_form_again() throws Exception {
        // name boş → validation hatası, form tekrar gösterilir
        mvc.perform(post("/customers")
                .param("name", "")
                .param("email", "x@test.com"))
           .andExpect(status().isOk())
           .andExpect(view().name("add-customer"));

        verify(customerService, never()).create(any());
    }

    @Test
    void delete_customer_redirects_with_flash() throws Exception {
        doNothing().when(customerService).delete(1L);

        mvc.perform(get("/customers/delete/{id}", 1L))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/customers"))
           .andExpect(flash().attributeExists("msg"));

        verify(customerService).delete(1L);
    }

  @Test
void update_customer_redirects_with_flash() throws Exception {
    // STUB YOK (gerekli değil)

    mvc.perform(post("/customers/update/{id}", 1L)
            .param("name", "Veli")
            .param("email", "veli@test.com"))
       .andExpect(status().is3xxRedirection())
       .andExpect(redirectedUrl("/customers"))
       .andExpect(flash().attribute("msg", containsString("Müşteri güncellendi")));

    verify(customerService).update(1L, "Veli", "veli@test.com");
}

}
