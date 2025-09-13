package com.example.staj.services;

import com.example.staj.controller.QuoteController;
import com.example.staj.entity.*;
import com.example.staj.repository.CarRepository;
import com.example.staj.repository.CustomerRepository;
import com.example.staj.repository.QuoteRepository;
import com.example.staj.services.PolicyService;
import com.example.staj.services.QuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QuoteControllerTest {

    private MockMvc mvc;

    @Mock QuoteService quoteService;
    @Mock QuoteRepository quotes;
    @Mock PolicyService policyService;
    @Mock CustomerRepository customerRepo;
    @Mock CarRepository carRepo;

    @InjectMocks QuoteController controller;
private Customer customer(long id, String name) {
    Customer c = new Customer();
    ReflectionTestUtils.setField(c, "id", id);   // <-- setter yoksa buradan
    c.setName(name);
    return c;
}

private Car car(long id, String plate, boolean active, Customer owner) {
    Car c = new Car();
    // Car'da setId varsa kullanabilirsin; yoksa aynı şekilde:
    // ReflectionTestUtils.setField(c, "id", id);
    ReflectionTestUtils.setField(c, "id", id);
    c.setPlate(plate);
    c.setActive(active);
    c.setCustomer(owner);
    return c;
}

private Quote pendingQuote(long id, String no) {
    Quote q = new Quote();
    ReflectionTestUtils.setField(q, "id", id);   // <-- setId olmadığı için buradan ver
    q.setQuoteNumber(no);
    q.setStatus(QuoteStatus.PENDING);
    return q;
}

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ---------- LIST / NEW FORM ----------

    @Test
    void list_returns_view_and_model_including_ok_err() throws Exception {
        when(quotes.findAll()).thenReturn(List.of(pendingQuote(1L, "Q-1")));

        mvc.perform(get("/quotes").flashAttr("ok", "done").flashAttr("err", ""))
           .andExpect(status().isOk())
           .andExpect(view().name("list-quotes"))
           .andExpect(model().attributeExists("quotes"))
           .andExpect(model().attribute("ok", is("done")));

        verify(quotes).findAll();
    }

    @Test
    void newForm_populates_defaults_and_lists() throws Exception {
        when(customerRepo.findAll()).thenReturn(List.of(customer(1L, "Ali")));
        when(carRepo.findAllByActiveTrue()).thenReturn(List.of(car(2L, "34ABC34", true, customer(1L, "Ali"))));

        mvc.perform(get("/quotes/new"))
           .andExpect(status().isOk())
           .andExpect(view().name("add-quote"))
           .andExpect(model().attributeExists("quote", "customers", "cars", "today", "defaultEnd"));
    }

    // ---------- CREATE ----------

    @Test
    void create_success_redirects_to_list_with_ok() throws Exception {
        var cust = customer(1L, "Ali");
        var vehicle = car(2L, "34ABC34", true, cust);
        when(carRepo.findByPlate("34ABC34")).thenReturn(Optional.of(vehicle));

        Quote created = pendingQuote(10L, "Q-10");
        when(quoteService.createQuote(eq(1L), eq(2L), eq("KASKO"),
                any(LocalDate.class), any(LocalDate.class), eq(7)))
            .thenReturn(created);

        mvc.perform(post("/quotes")
                .param("customerId", "1")
                .param("carPlate", "34ABC34")
                .param("product", "KASKO")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-12-31")
                .param("validDays", "7"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/quotes"))
           .andExpect(flash().attribute("ok", containsString("Teklif oluşturuldu: Q-10")));

        verify(quoteService).createQuote(eq(1L), eq(2L), eq("KASKO"),
                any(LocalDate.class), any(LocalDate.class), eq(7));
    }

    @Test
    void create_invalid_dates_redirects_back_with_err() throws Exception {
        mvc.perform(post("/quotes")
                .param("customerId", "1")
                .param("carPlate", "34ABC34")
                .param("product", "KASKO")
                .param("startDate", "2025-12-31")
                .param("endDate", "2025-01-01")) // start > end
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/quotes/new"))
           .andExpect(flash().attribute("err",
                   containsString("Başlangıç tarihi, bitişten büyük olamaz")));
        verifyNoInteractions(quoteService);
    }

    @Test
    void create_car_not_found_redirects_with_err() throws Exception {
        when(carRepo.findByPlate("XX")).thenReturn(Optional.empty());

        mvc.perform(post("/quotes")
                .param("customerId", "1")
                .param("carPlate", "XX")
                .param("product", "KASKO")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-12-31"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/quotes/new"))
           .andExpect(flash().attribute("err", containsString("Plakaya ait araç bulunamadı")));
    }

    @Test
    void create_car_belongs_to_other_customer_redirects_with_err() throws Exception {
        var owner = customer(2L, "Veli");
        when(carRepo.findByPlate("34ABC34")).thenReturn(Optional.of(car(2L, "34ABC34", true, owner)));

        mvc.perform(post("/quotes")
                .param("customerId", "1")          // farklı müşteri
                .param("carPlate", "34ABC34")
                .param("product", "KASKO")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-12-31"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/quotes/new"))
           .andExpect(flash().attribute("err",
                   containsString("Seçilen araç bu müşteriye ait değildir.")));
    }
    
    @Test
    void create_inactive_car_redirects_with_err() throws Exception {
        var cust = customer(1L, "Ali");
        when(carRepo.findByPlate("34ABC34"))
            .thenReturn(Optional.of(car(2L, "34ABC34", false, cust))); // pasif

        mvc.perform(post("/quotes")
                .param("customerId", "1")
                .param("carPlate", "34ABC34")
                .param("product", "KASKO")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-12-31"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/quotes/new"))
           .andExpect(flash().attribute("err",
                   containsString("Araç pasif")));
    }

    // ---------- DELETE ----------

    @Test
    void delete_pending_quote_success() throws Exception {
        Quote q = pendingQuote(5L, "Q-5");
        when(quotes.findById(5L)).thenReturn(Optional.of(q));

        mvc.perform(post("/quotes/{id}/delete", 5L))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/quotes"))
           .andExpect(flash().attribute("ok", containsString("Teklif silindi: Q-5")));

        verify(quotes).delete(q);
    }

       @Test
    void delete_only_pending_allowed_else_err() throws Exception {
    Quote q = pendingQuote(6L, "Q-6");
    // PENDING dışı durum verelim
    q.setStatus(null); // veya q.setStatus(QuoteStatus.REJECTED) varsa
    when(quotes.findById(6L)).thenReturn(Optional.of(q));

    mvc.perform(post("/quotes/{id}/delete", 6L))
       .andExpect(status().is3xxRedirection())
       .andExpect(redirectedUrl("/quotes"))
       .andExpect(flash().attribute("err",
           containsString("Sadece onaylanmamış"))); // mesajı birebir gör

    verify(quotes, never()).delete(any());
}



    @Test
    void delete_not_found_err() throws Exception {
        when(quotes.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(post("/quotes/{id}/delete", 99L))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/quotes"))
           .andExpect(flash().attribute("err", containsString("Teklif bulunamadı")));
    }

    // ---------- ACCEPT ----------

    @Test
    void accept_success_redirects_to_policy_detail_with_ok() throws Exception {
        Policy p = new Policy();
        ReflectionTestUtils.setField(p, "id", 42L);   // setId yoksa böyle
        p.setPolicyNumber("POL-42");
        when(policyService.acceptFromQuote(7L)).thenReturn(p);

        mvc.perform(post("/quotes/{id}/accept", 7L))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/policies/42"))
           .andExpect(flash().attribute("ok", containsString("Poliçe No: POL-42")));
    }

    @Test
    void accept_error_redirects_back_with_err() throws Exception {
        when(policyService.acceptFromQuote(8L))
            .thenThrow(new IllegalStateException("Geçersiz durum"));

        mvc.perform(post("/quotes/{id}/accept", 8L))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/quotes"))
           .andExpect(flash().attribute("err", containsString("Geçersiz durum")));
    }
}
