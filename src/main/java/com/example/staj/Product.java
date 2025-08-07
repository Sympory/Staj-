package com.example.staj;

import jakarta.persistence.*;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private Double price;

    // Getter ve Setter metotları (veya Lombok kullanıyorsan @Data)
}
