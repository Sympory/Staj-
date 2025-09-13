package com.example.staj.services;

import com.example.staj.entity.Car;
import com.example.staj.entity.Customer;
import com.example.staj.entity.Quote;
import com.example.staj.entity.QuoteStatus;
import com.example.staj.repository.CarRepository;
import com.example.staj.repository.CustomerRepository;
import com.example.staj.repository.QuoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock QuoteRepository quoteRepo;
    @Mock CustomerRepository customerRepo;
    @Mock CarRepository carRepo;
    @Mock PricingService pricing;

    @InjectMocks QuoteService quoteService;

    private Customer customer(long id, String name) {
        Customer c = new Customer();
        // id gerekmez; davranışı etkilemiyor
        c.setName(name);
        return c;
    }

    private Car car(long id, String plate) {
        Car c = new Car();
        c.setPlate(plate);
        return c;
    }

    @Test
    void createQuote_happyPath_saves_withComputedPremiums_callsPricingTwice() {
        // arrange
        var start = LocalDate.now().plusDays(1);
        var end   = start.plusDays(30);

        var cust = customer(1L, "Ali");
        var vehicle = car(2L, "34ABC34");

        when(customerRepo.findById(1L)).thenReturn(Optional.of(cust));
        when(carRepo.findById(2L)).thenReturn(Optional.of(vehicle));

        // price ilk çağrıda p1, priceQuote içinde ikinci çağrıda p2 dönsün (sonuç p2 ile kalmalı)
        var p1 = new PricingService.Pricing(new BigDecimal("100.00"),
                                            new BigDecimal("10.00"),
                                            new BigDecimal("110.00"));
        var p2 = new PricingService.Pricing(new BigDecimal("200.00"),
                                            new BigDecimal("20.00"),
                                            new BigDecimal("220.00"));
        when(pricing.price(eq(vehicle), eq("KASKO"), eq(start), eq(end)))
                .thenReturn(p1)  // createQuote içindeki ilk hesap
                .thenReturn(p2); // priceQuote içindeki ikinci hesap

        // save geri dönen nesneyi aynen iade etsin
        ArgumentCaptor<Quote> cap = ArgumentCaptor.forClass(Quote.class);
        when(quoteRepo.save(any(Quote.class))).thenAnswer(inv -> inv.getArgument(0));

        // act
        Quote saved = quoteService.createQuote(1L, 2L, "KASKO", start, end, 7);

        // assert – repo.save’e giden değeri yakala
        verify(quoteRepo).save(cap.capture());
        Quote q = cap.getValue();

        assertThat(q.getCustomer()).isSameAs(cust);
        assertThat(q.getCar()).isSameAs(vehicle);
        assertThat(q.getProduct()).isEqualTo("KASKO");
        assertThat(q.getCoverageStart()).isEqualTo(start);
        assertThat(q.getCoverageEnd()).isEqualTo(end);
        assertThat(q.getStatus()).isEqualTo(QuoteStatus.PENDING);
        assertThat(q.getQuoteNumber()).startsWith("Q-");

        // İkinci fiyatlama sonuçları geçerli olmalı
        assertThat(q.getNetPremium()).isEqualByComparingTo("200.00");
        assertThat(q.getTax()).isEqualByComparingTo("20.00");
        assertThat(q.getGrossPremium()).isEqualByComparingTo("220.00");

        // validUntil ~ şimdi+7gün (tolerans payı ile)
        assertThat(q.getValidUntil()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(q.getValidUntil()).isBefore(LocalDateTime.now().plusDays(8));

        // pricing iki kez çağrılır (create + priceQuote)
        verify(pricing, times(2)).price(eq(vehicle), eq("KASKO"), eq(start), eq(end));

        // metod geri dönüşü de save edilen quote
        assertThat(saved).isSameAs(q);
    }

    @Test
    void createQuote_startInPast_throws() {
        var start = LocalDate.now().minusDays(1);
        var end = LocalDate.now().plusDays(10);

        assertThatThrownBy(() ->
            quoteService.createQuote(1L, 2L, "KASKO", start, end, 7)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Geçmiş tarihli başlangıç seçilemez");
    }

    @Test
    void createQuote_endBeforeStart_throws() {
        var start = LocalDate.now().plusDays(10);
        var end = LocalDate.now().plusDays(5);

        assertThatThrownBy(() ->
            quoteService.createQuote(1L, 2L, "KASKO", start, end, 7)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Bitiş tarihi başlangıçtan önce olamaz");
    }

    @Test
    void createQuote_customerNotFound_throws() {
        var start = LocalDate.now().plusDays(1);
        var end = start.plusDays(10);
        when(customerRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            quoteService.createQuote(1L, 2L, "KASKO", start, end, 7)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Customer not found");
    }

    @Test
    void createQuote_carNotFound_throws() {
        var start = LocalDate.now().plusDays(1);
        var end = start.plusDays(10);
        when(customerRepo.findById(1L)).thenReturn(Optional.of(new Customer()));
        when(carRepo.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            quoteService.createQuote(1L, 2L, "KASKO", start, end, 7)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Car not found");
    }

    @Test
    void expirePending_delegatesToRepo() {
        when(quoteRepo.expireAllPendingBefore(any(LocalDateTime.class))).thenReturn(3);

        int n = quoteService.expirePending();

        assertThat(n).isEqualTo(3);
        verify(quoteRepo).expireAllPendingBefore(any(LocalDateTime.class));
    }
}
