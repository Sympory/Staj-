package com.example.staj.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.staj.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {}
