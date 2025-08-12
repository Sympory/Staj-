package com.example.staj;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "cars")
public class Car {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 20)
    private String plate;              // plaka

    @Size(max = 50) private String brand;
    @Size(max = 50) private String model;
    private Integer modelYear;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name="fk_car_customer"))
    private Customer customer;

    // getters/setters
    public Long getId() { return id; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getModelYear() { return modelYear; }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
}
