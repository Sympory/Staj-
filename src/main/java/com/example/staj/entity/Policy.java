package com.example.staj.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "policy")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Poliçe numarası boş olamaz")
    @Column(name = "policy_number", nullable = false, length = 50, unique = true)
    private String policyNumber;

    @NotNull(message = "Başlangıç tarihi gerekli")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "Bitiş tarihi gerekli")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // Var olan kodunla uyum için bırakıyoruz
    @Column(name = "active", nullable = false)
    private boolean active = true;

    // İsteğe bağlı ama tavsiye edilen durum alanı
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PolicyStatus status = PolicyStatus.ACTIVE;

    // Fiyat alanları
    @Column(name = "premium", precision = 18, scale = 2)
    private BigDecimal premium;

    @Column(name = "tax", precision = 18, scale = 2)
    private BigDecimal tax;

    @Column(name = "total", precision = 18, scale = 2)
    private BigDecimal total;

    // MÜŞTERİ
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_policy_customer"))
    private Customer customer;

    // ARAÇ
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_policy_car"))
    private Car car;

    // Hangi Tekliften üretildi?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id",
            foreignKey = @ForeignKey(name = "fk_policy_quote"))
    private Quote fromQuote;

    // --- GETTERS / SETTERS ---

    public Long getId() { return id; }

    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public PolicyStatus getStatus() { return status; }
    public void setStatus(PolicyStatus status) { this.status = status; }

    public BigDecimal getPremium() { return premium; }
    public void setPremium(BigDecimal premium) { this.premium = premium; }

    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }

    public Quote getFromQuote() { return fromQuote; }
    public void setFromQuote(Quote fromQuote) { this.fromQuote = fromQuote; }
}
