package com.example.staj;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "İsim boş olamaz")
    @Size(min = 2, max = 50, message = "İsim 2-50 karakter olmalı")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "İsim yalnızca harf ve boşluk içerebilir")
    private String name;
    @OneToMany(mappedBy = "customer")
    private java.util.List<Car> cars = new java.util.ArrayList<>();


    @NotBlank(message = "Email boş olamaz")
    @Email(message = "Geçerli bir email girin")
    private String email;

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
