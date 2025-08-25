package com.example.staj.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.staj.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
