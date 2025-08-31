package com.example.staj.entity;

import jakarta.persistence.*;



@Entity
@Table(name = "car_model", uniqueConstraints = {
        @UniqueConstraint(name = "uk_model_per_brand", columnNames = {"brand_id","name"})
})
public class CarModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    
        @Override
        public String toString() {
            return name;
        }

    @Column(nullable = false, length = 80)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_model_brand"))
    private Brand brand;

    // --- GETTER & SETTER ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }

    // --- CONSTRUCTORS ---
    public CarModel() {}
    public CarModel(String name, Brand brand) {
        this.name = name;
        this.brand = brand;
    }
}
