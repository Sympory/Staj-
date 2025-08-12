package com.example.staj.repository;

import com.example.staj.Car;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, Long> {
    Optional<Car> findByPlate(String plate);
    List<Car> findByCustomerId(Long customerId);
}
