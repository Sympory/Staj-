package com.example.staj.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "brand", uniqueConstraints = {
        @UniqueConstraint(name = "uk_brand_name", columnNames = {"name"})
})
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
@Override
public String toString() {
    return name; // brand tablosundaki "name" alanı
}

    @Column(nullable = false, length = 80)
    private String name;

    // --- GETTER & SETTER ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // --- CONSTRUCTORS ---
    public Brand() {}
    public Brand(String name) { this.name = name; }
}
