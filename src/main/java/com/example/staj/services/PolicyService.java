package com.example.staj.services;

import com.example.staj.entity.Policy;
import com.example.staj.entity.PolicyStatus;
import com.example.staj.entity.Quote;
import com.example.staj.entity.QuoteStatus;
import com.example.staj.repository.PolicyRepository;
import com.example.staj.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PolicyService {

    private final QuoteRepository quoteRepo;
    private final PolicyRepository policyRepo;

    public PolicyService(QuoteRepository quoteRepo, PolicyRepository policyRepo) {
        this.quoteRepo = quoteRepo;
        this.policyRepo = policyRepo;
    }

    @Transactional
    public Long acceptFromQuote(Long quoteId) {
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

        policyRepo.save(p);

        // ✅ Quote güncelle
        q.setStatus(QuoteStatus.APPROVED);
        q.setAcceptedAt(LocalDateTime.now());
        quoteRepo.save(q);

        return p.getId();
    }
     // ✅ Pasif poliçeyi AKTİFLEŞTİR – overlap kontrolü ile
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
      // ✅ Aktif poliçe silinmesin (kural), sadece pasif olan silinsin
    @Transactional
    public void deletePolicy(Long policyId) {
        Policy p = policyRepo.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Poliçe bulunamadı"));
        if (p.isActive()) {
            throw new IllegalStateException("Aktif poliçe silinemez. Önce iptal edin.");
        }
        policyRepo.deleteById(policyId);
    }
    // ✅ Yeni yardımcı metod: poliçe iptal etme
    @Transactional
    public void cancelPolicy(Long policyId) {
        int updated = policyRepo.deactivatePolicy(policyId);
        if (updated == 0) {
            throw new IllegalArgumentException("Policy not found or already inactive");
        }
    }
}
