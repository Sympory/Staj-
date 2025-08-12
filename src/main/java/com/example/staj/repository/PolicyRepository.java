package com.example.staj.repository;

import com.example.staj.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    @Query("""
        select p from Policy p
        where (:q is null or
               lower(p.policyNumber) like lower(concat('%', :q, '%')) or
               lower(p.customer.name) like lower(concat('%', :q, '%')) or
               lower(p.car.plate)     like lower(concat('%', :q, '%')))
          and (:active is null or p.active = :active)
          and (:customerId is null or p.customer.id = :customerId)
          and (:carId is null or p.car.id = :carId)
          and (:startFrom is null or p.startDate >= :startFrom)
          and (:endTo    is null or p.endDate   <= :endTo)
        """)
   org.springframework.data.domain.Page<Policy> search(
  @org.springframework.data.repository.query.Param("q") String q,
  @org.springframework.data.repository.query.Param("active") Boolean active,
  @org.springframework.data.repository.query.Param("customerId") Long customerId,
  @org.springframework.data.repository.query.Param("carId") Long carId,
  @org.springframework.data.repository.query.Param("startFrom") java.time.LocalDate startFrom,
  @org.springframework.data.repository.query.Param("endTo") java.time.LocalDate endTo,
  org.springframework.data.domain.Pageable pageable
);
}
