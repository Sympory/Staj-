// src/main/java/com/example/staj/services/PolicyService.java
package com.example.staj.services;

import com.example.staj.entity.Policy;
import com.example.staj.entity.PolicyStatus;
import com.example.staj.entity.Quote;
import com.example.staj.entity.QuoteStatus;
import com.example.staj.repository.PolicyRepository;
import com.example.staj.repository.QuoteRepository;
import com.example.staj.repository.CustomerRepository;
import com.example.staj.repository.CarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;


@Service
public class PolicyService {

    private final QuoteRepository quoteRepo;
    private final PolicyRepository policyRepo;
    private final CustomerRepository customerRepo;
    private final CarRepository carRepo;

    public PolicyService(QuoteRepository quoteRepo,
                         PolicyRepository policyRepo,
                         CustomerRepository customerRepo,
                         CarRepository carRepo) {
        this.quoteRepo = quoteRepo;
        this.policyRepo = policyRepo;
        this.customerRepo = customerRepo;
        this.carRepo = carRepo;
    }
        @Transactional(readOnly = true)
        public long countByCar(Long carId) {
            return policyRepo.countByCarId(carId);
        }

    // --- CREATE (controller -> service) ---
    @Transactional
    public Long create(Long customerId,
                       Long carId,
                       String policyNumber,
                       LocalDate startDate,
                       LocalDate endDate,
                       boolean active) {

        var customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Müşteri bulunamadı: " + customerId));

        var p = new Policy();
        p.setPolicyNumber(policyNumber);
        p.setCustomer(customer);
        p.setStartDate(startDate);
        p.setEndDate(endDate);
        p.setActive(active);


        if (active) {
            p.setStatus(PolicyStatus.ACTIVE);
        }
        // else: status boş kalabilir veya enum’ınıza göre DRAFT/PENDING set edebilirsiniz

        if (carId != null) {
            var car = carRepo.findById(carId)
                    .orElseThrow(() -> new IllegalArgumentException("Araç bulunamadı: " + carId));
            p.setCar(car);

            // Aktif açılıyorsa overlap kontrolü yap
            if (active) {
                long overlaps = policyRepo.countActiveOverlaps(car.getId(), startDate, endDate);
                if (overlaps > 0) {
                    throw new IllegalStateException("Bu araç için bu tarihlerde aktif poliçe zaten var.");
                }
            }
        }

        return policyRepo.save(p).getId();
    }

    // PolicyService.java
@Transactional(readOnly = true)
public Policy get(Long id) {
    return policyRepo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Poliçe bulunamadı: " + id));
}

   // Önce imzayı değiştir:
@Transactional
public Policy acceptFromQuote(Long quoteId) {
    Quote q = quoteRepo.findById(quoteId)
            .orElseThrow(() -> new IllegalArgumentException("Quote not found"));

    if (q.getStatus() != QuoteStatus.PENDING) {
        throw new IllegalStateException("Quote is not pending");
    }
    if (q.getValidUntil() != null && q.getValidUntil().isBefore(LocalDateTime.now())) {
        throw new IllegalStateException("Quote expired");
    }

    long overlaps = policyRepo.countActiveOverlaps(
            q.getCar().getId(),
            q.getCoverageStart(),
            q.getCoverageEnd()
    );
    if (!q.getCar().isActive()) {
    throw new IllegalStateException("Araç pasif. Poliçe oluşturulamaz. Lütfen aracı aktifleştirin.");
}

    if (overlaps > 0) {
        throw new IllegalStateException("Bu araç için seçilen tarihlerde aktif bir poliçe zaten var.");
    }

    Policy p = new Policy();
    p.setPolicyNumber("P-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    p.setCustomer(q.getCustomer());
    p.setCar(q.getCar());
    p.setStartDate(q.getCoverageStart());
    p.setEndDate(q.getCoverageEnd());
    p.setPremium(q.getNetPremium());
    p.setTax(q.getTax());
    p.setTotal(q.getGrossPremium());
    p.setFromQuote(q);
    p.setStatus(PolicyStatus.ACTIVE);
    p.setActive(true);

    Policy saved = policyRepo.save(p);

    q.setStatus(QuoteStatus.APPROVED);
    q.setAcceptedAt(LocalDateTime.now());
    quoteRepo.save(q);

    return saved; // <-- Artık Policy dönüyoruz
}

@Transactional(readOnly = true)
public long countActiveByCar(Long carId) {
    return policyRepo.countByCarIdAndActiveTrue(carId);
}

    // --- SEARCH ---
    @Transactional(readOnly = true)
    public Page<Policy> search(String q, Boolean active, Long customerId, Long carId,
                               LocalDate startFrom, LocalDate endTo, Pageable pageable) {
        return policyRepo.search(
                (q == null || q.isBlank()) ? null : q.trim(),
                active, customerId, carId, startFrom, endTo, pageable
        );
    }

    // --- ACTIVATE ---
    @Transactional
    public void activatePolicy(Long policyId) {
        Policy p = policyRepo.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Poliçe bulunamadı"));
        if (p.isActive()) {
            throw new IllegalStateException("Poliçe zaten aktif.");
        }
        long overlaps = policyRepo.countActiveOverlapsExcept(
                p.getCar().getId(), p.getStartDate(), p.getEndDate(), p.getId());
        if (overlaps > 0) {
            throw new IllegalStateException("Bu tarihlerde aynı araç için aktif poliçe var. Aktifleştirilemez.");
        }
        p.setActive(true);
        p.setStatus(PolicyStatus.ACTIVE);
        policyRepo.save(p);
    }

    // --- DELETE (sadece pasif) ---
    @Transactional
    public void deletePolicy(Long policyId) {
        Policy p = policyRepo.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Poliçe bulunamadı"));
        if (p.isActive()) {
            throw new IllegalStateException("Aktif poliçe silinemez. Önce iptal edin.");
        }
        policyRepo.deleteById(policyId);
    }

    // --- CANCEL ---
    @Transactional
    public void cancelPolicy(Long policyId) {
        int updated = policyRepo.deactivatePolicy(policyId);
        if (updated == 0) {
            throw new IllegalArgumentException("Policy not found or already inactive");
        }
    }
}
