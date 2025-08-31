package com.example.staj.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "cars")
public class Car {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer modelYear;

    
    @NotBlank @Size(max = 20)
    private String plate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false,
            foreignKey = @ForeignKey(name="fk_car_brand"))
    private Brand brand;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false,
            foreignKey = @ForeignKey(name="fk_car_model"))
    private CarModel model;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name="fk_car_customer"))
    private Customer customer;
      // ✅ Soft delete alanı
    @Column(nullable = false)
    private boolean active = true;

    // getters/setters ...
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    // --- GET/SET ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getModelYear() { return modelYear; }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }      // ✅ DOĞRU İMZA

    public CarModel getModel() { return model; }
    public void setModel(CarModel model) { this.model = model; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    @PrePersist @PreUpdate
    private void normalizePlate() {
        if (plate != null) {
            plate = plate.trim().toUpperCase().replaceAll("\\s+", " ");
        }
    }
    public Car() {}
    public Car(String plate, Brand brand, CarModel model, Integer modelYear, Customer customer) {
        this.plate = plate;
        this.brand = brand;
        this.model = model;
        this.modelYear = modelYear;
        this.customer = customer;
    }
}
