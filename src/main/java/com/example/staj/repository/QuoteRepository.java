package com.example.staj.repository;

import com.example.staj.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

  Optional<Quote> findByQuoteNumber(String quoteNumber);

  
    long countByCar_Id(Long carId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Quote q where q.car.id = :carId")
    int deleteAllByCarId(@Param("carId") Long carId);
    
  @Modifying
  @Query("""
    update Quote q
       set q.status = com.example.staj.entity.QuoteStatus.EXPIRED
     where q.status = com.example.staj.entity.QuoteStatus.PENDING
       and q.validUntil < :now
  """)
  int expireAllPendingBefore(@Param("now") LocalDateTime now);
}
