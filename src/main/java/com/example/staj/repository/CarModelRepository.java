// src/main/java/com/example/staj/repository/CarModelRepository.java
package com.example.staj.repository;

import com.example.staj.entity.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CarModelRepository extends JpaRepository<CarModel, Long> {
    List<CarModel> findByBrand_IdOrderByNameAsc(Long brandId);



}
