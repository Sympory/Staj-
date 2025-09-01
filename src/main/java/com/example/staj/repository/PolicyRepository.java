package com.example.staj.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.example.staj.entity.Policy;
import com.example.staj.entity.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;



public interface PolicyRepository extends JpaRepository<Policy, Long> {

  // ---- LISTELEME / ARAMA ----
  @Query("""
    select p from Policy p
    where (:q is null or
           lower(p.policyNumber) like lower(concat('%', :q, '%')) or
           lower(p.customer.name) like lower(concat('%', :q, '%')) or
           lower(p.car.plate)     like lower(concat('%', :q, '%')) )
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

  // ---- OVERLAP KONTROLLERİ ----
  @Query("""
    select count(p) from Policy p
     where p.car.id = :carId
       and p.active = true
       and not (p.endDate < :startDate or p.startDate > :endDate)
  """)

  long countActiveOverlaps(@Param("carId") Long carId,
                           @Param("startDate") LocalDate startDate,
                           @Param("endDate") LocalDate endDate);


  @Query("""
    select count(p) from Policy p
     where p.car.id = :carId
       and p.active = true
       and p.id <> :excludeId
       and not (p.endDate < :startDate or p.startDate > :endDate)
  """)
  long countActiveOverlapsExcept(@Param("carId") Long carId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate,
                                 @Param("excludeId") Long excludeId);

  long countByCarId(Long carId);
  List<Policy> findAllByCarIdAndStatusIn(Long carId, Collection<PolicyStatus> statuses);

  // sadece AKTİF poliçe sayısı
  long countByCarIdAndActiveTrue(Long carId);
    // pasif (active=false) poliçeleri topluca sil
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from Policy p where p.car.id = :carId and p.active = false")
  int deleteAllPassiveByCarId(@Param("carId") Long carId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
         update Policy p
            set p.active = false,
                p.status = :passive
          where p.car.id = :carId
            and (p.active = true or p.status in :toClose)
         """)
  int passivateAllByCar(@Param("carId") Long carId,
                        @Param("passive") PolicyStatus passive,
                        @Param("toClose") Collection<PolicyStatus> toClose);

  // ---- TEK POLİÇE DURUM GÜNCELLEME ----
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update Policy p set p.active = false, p.status = com.example.staj.entity.PolicyStatus.CANCELLED where p.id = :id")
  int deactivatePolicy(@Param("id") Long id);
}
