package com.example.staj.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quotes")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="quote_number", nullable=false, length=64, unique=true)
    private String quoteNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_quote_customer"))
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_quote_car"))
    private Car car;

    @Column(name = "has_previous_claims", nullable = false)
    private Boolean hasPreviousClaims = false;

    @Column(nullable = false, length = 50)
    private String product;

    @Column(name = "coverage_start", nullable = false)
    private LocalDate coverageStart;

    @Column(name = "coverage_end", nullable = false)
    private LocalDate coverageEnd;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuoteStatus status;

    @Column(name = "net_premium", nullable = false, precision = 18, scale = 2)
    private BigDecimal netPremium;

    @Column(name = "tax", nullable = false, precision = 18, scale = 2)
    private BigDecimal tax;

    @Column(name = "gross_premium", nullable = false, precision = 18, scale = 2)
    private BigDecimal grossPremium;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @PrePersist
    public void ensureQuoteNumber() {
        if (quoteNumber == null || quoteNumber.isBlank()) {
            this.quoteNumber = "Q-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
    

    public Boolean getHasPreviousClaims() { return hasPreviousClaims; }
    public void setHasPreviousClaims(Boolean hasPreviousClaims) { this.hasPreviousClaims = hasPreviousClaims; }
    // ---- getters/setters ----
    public Long getId() { return id; }

    public String getQuoteNumber() { return quoteNumber; }
    public void setQuoteNumber(String quoteNumber) { this.quoteNumber = quoteNumber; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public LocalDate getCoverageStart() { return coverageStart; }
    public void setCoverageStart(LocalDate coverageStart) { this.coverageStart = coverageStart; }

    public LocalDate getCoverageEnd() { return coverageEnd; }
    public void setCoverageEnd(LocalDate coverageEnd) { this.coverageEnd = coverageEnd; }

    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }

    public QuoteStatus getStatus() { return status; }
    public void setStatus(QuoteStatus status) { this.status = status; }

    public BigDecimal getNetPremium() { return netPremium; }
    public void setNetPremium(BigDecimal netPremium) { this.netPremium = netPremium; }

    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }

    public BigDecimal getGrossPremium() { return grossPremium; }
    public void setGrossPremium(BigDecimal grossPremium) { this.grossPremium = grossPremium; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
}
