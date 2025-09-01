package com.example.staj.repository;

import com.example.staj.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CarRepository extends JpaRepository<Car, Long> {
    Optional<Car> findByPlate(String plate);
    List<Car> findAllByCustomerIdOrderByPlateAsc(Long customerId);
 // Sadece aktif arabalar
    List<Car> findAllByActiveTrue();
    
    boolean existsByPlateIgnoreCase(String plate);
    boolean existsByPlateIgnoreCaseAndIdNot(String plate, Long id);
     // ✅ Filtreli arama (q, müşteri, marka, model, yıl aralığı) + sayfalama
    @Query("""
      select c from Car c
       where (:q is null or
              lower(c.plate)         like lower(concat('%', :q, '%')) or
              lower(c.customer.name) like lower(concat('%', :q, '%')) or
              lower(c.brand.name)    like lower(concat('%', :q, '%')) or
              lower(c.model.name)    like lower(concat('%', :q, '%')))
         and (:customerId is null or c.customer.id = :customerId)
         and (:brandId    is null or c.brand.id    = :brandId)
         and (:modelId    is null or c.model.id    = :modelId)
         and (:yearFrom   is null or c.modelYear  >= :yearFrom)
         and (:yearTo     is null or c.modelYear  <= :yearTo)
    """)
    Page<Car> search(@Param("q") String q,
                     @Param("customerId") Long customerId,
                     @Param("brandId") Long brandId,
                     @Param("modelId") Long modelId,
                     @Param("yearFrom") Integer yearFrom,
                     @Param("yearTo") Integer yearTo,
                     Pageable pageable);
}

