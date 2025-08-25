package com.example.staj.repository;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.*;

import com.example.staj.entity.Policy;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

  @Query("""
    select p from Policy p
    where (:q is null or
           lower(p.policyNumber) like lower(concat('%', :q, '%')) or
           lower(p.customer.name) like lower(concat('%', :q, '%')) or
           lower(p.car.plate)     like lower(concat('%', :q, '%')))
      and (:active     is null or p.active = :active)
      and (:customerId is null or p.customer.id = :customerId)
      and (:carId      is null or p.car.id = :carId)
      and (:startFrom  is null or p.startDate >= :startFrom)
      and (:endTo      is null or p.endDate   <= :endTo)
  """)
  Page<Policy> search(@Param("q") String q,
                      @Param("active") Boolean active,
                      @Param("customerId") Long customerId,
                      @Param("carId") Long carId,
                      @Param("startFrom") LocalDate startFrom,
                      @Param("endTo") LocalDate endTo,
                      Pageable pageable);

  @Query("""
    select count(p) from Policy p
     where p.car.id = :carId
       and p.active = true
       and p.startDate <= :endDate
       and p.endDate   >= :startDate
  """)
  
  long countActiveOverlaps(@Param("carId") Long carId,
                           @Param("startDate") LocalDate startDate,
                           @Param("endDate") LocalDate endDate);

  // ✅ Bu poliçe hariç overlap say (Aktifleştirirken kullanacağız)
  @Query("""
    select count(p) from Policy p
     where p.car.id = :carId
       and p.active = true
       and p.id <> :excludeId
       and not (p.endDate < :start or p.startDate > :end)
  """)
  long countActiveOverlapsExcept(@Param("carId") Long carId,
                                 @Param("start") LocalDate start,
                                 @Param("end") LocalDate end,
                                 @Param("excludeId") Long excludeId);
  // --- YENİ EKLE ---
  @Modifying
  @Query("update Policy p set p.active = false, p.status = com.example.staj.entity.PolicyStatus.CANCELLED where p.id = :id")
  int deactivatePolicy(@Param("id") Long id);
}
