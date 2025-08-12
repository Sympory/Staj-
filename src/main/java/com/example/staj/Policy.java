package com.example.staj;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "policy") // DB'de tablo adı 'policy'
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Poliçe numarası boş olamaz")
    @Column(name = "policy_number", nullable = false, length = 50)
    private String policyNumber;

    @NotNull(message = "Başlangıç tarihi gerekli")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "Bitiş tarihi gerekli")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "active")
    private boolean active = true;

    // MÜŞTERİ BAĞI
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_policy_customer"))
    private Customer customer;

    // ARABA BAĞI (cars tablosu ve policy.car_id için ddl-auto=update ya da ALTER TABLE gerekir)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_policy_car"))
    private Car car;

    // --- getters / setters ---
    public Long getId() { return id; }

    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }
}
