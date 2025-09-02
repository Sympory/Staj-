package com.example.staj.services;

import com.example.staj.entity.Quote;
import com.example.staj.entity.QuoteStatus;
import com.example.staj.repository.CarRepository;
import com.example.staj.repository.CustomerRepository;
import com.example.staj.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class QuoteService {

    private final QuoteRepository quotes;
    private final CustomerRepository customers;
    private final CarRepository cars;
    private final PricingService pricing;

    public QuoteService(QuoteRepository quotes,
                        CustomerRepository customers,
                        CarRepository cars,
                        PricingService pricing) {
        this.quotes = quotes;
        this.customers = customers;
        this.cars = cars;
        this.pricing = pricing;
    }

        /** Tek bir teklifi yeniden fiyatlandır (merkez nokta) */
    public void priceQuote(Quote q) {
        var p = pricing.price(q.getCar(), q.getProduct(), q.getCoverageStart(), q.getCoverageEnd());
        q.setNetPremium(p.net());
        q.setTax(p.tax());
        q.setGrossPremium(p.gross());
    }

    @Transactional
    public Quote createQuote(Long customerId,
                             Long carId,
                             String product,
                             LocalDate start,
                             LocalDate end,
                             int validDays) {
                                LocalDate today = LocalDate.now();
    if (start.isBefore(today)) {
        throw new IllegalArgumentException("Geçmiş tarihli başlangıç seçilemez.");
    }
    if (end.isBefore(start)) {
        throw new IllegalArgumentException("Bitiş tarihi başlangıçtan önce olamaz.");
    }


        var customer = customers.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        var car = cars.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("Car not found"));

        var p = pricing.price(car, product, start, end);

        Quote q = new Quote();
        q.setQuoteNumber("Q-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        q.setCustomer(customer);
        q.setCar(car);
        q.setProduct(product);
        q.setCoverageStart(start);
        q.setCoverageEnd(end);
        q.setValidUntil(LocalDateTime.now().plusDays(validDays));
        q.setNetPremium(p.net());
        q.setTax(p.tax());
        q.setGrossPremium(p.gross());
        q.setStatus(QuoteStatus.PENDING);
        priceQuote(q);
        return quotes.save(q);
    }

    @Transactional
    public int expirePending() {
        return quotes.expireAllPendingBefore(LocalDateTime.now());
    }
}
