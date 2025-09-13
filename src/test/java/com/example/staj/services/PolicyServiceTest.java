package com.example.staj.services;

import com.example.staj.entity.*;
import com.example.staj.repository.CarRepository;
import com.example.staj.repository.CustomerRepository;
import com.example.staj.repository.PolicyRepository;
import com.example.staj.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock QuoteRepository quoteRepo;
    @Mock PolicyRepository policyRepo;
    @Mock CustomerRepository customerRepo;
    @Mock CarRepository carRepo;

    @InjectMocks PolicyService policyService;

    private Customer cust;
    private Car car;

    @BeforeEach
    void init() {
        cust = new Customer();
        cust.setName("Ali");
        ReflectionTestUtils.setField(cust, "id", 1L);

        car = new Car();
        car.setPlate("34ABC34");
        car.setActive(true);
        ReflectionTestUtils.setField(car, "id", 2L);
    }

    // ---------- create(...) ----------

    @Test
    void create_active_with_car_no_overlap_returns_id_and_sets_status_active() {
        var start = LocalDate.of(2025,1,1);
        var end   = LocalDate.of(2025,12,31);

        when(customerRepo.findById(1L)).thenReturn(Optional.of(cust));
        when(carRepo.findById(2L)).thenReturn(Optional.of(car));
        when(policyRepo.countActiveOverlaps(2L, start, end)).thenReturn(0L);

        // save edilen policy'nin id'sini 42 yapalım
        when(policyRepo.save(any(Policy.class))).thenAnswer(inv -> {
            Policy p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 42L);
            return p;
        });

        Long id = policyService.create(1L, 2L, "POL-XX", start, end, true);

        assertThat(id).isEqualTo(42L);
        // kaydedilen policy'yi yakala ve ACTIVE olduğuna bakalım
        ArgumentCaptor<Policy> cap = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepo).save(cap.capture());
        Policy saved = cap.getValue();
        assertThat(saved.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCar()).isSameAs(car);
        assertThat(saved.getCustomer()).isSameAs(cust);
    }

    @Test
    void create_active_with_overlap_throws() {
        var start = LocalDate.of(2025,1,1);
        var end   = LocalDate.of(2025,12,31);

        when(customerRepo.findById(1L)).thenReturn(Optional.of(cust));
        when(carRepo.findById(2L)).thenReturn(Optional.of(car));
        when(policyRepo.countActiveOverlaps(2L, start, end)).thenReturn(3L);

        assertThatThrownBy(() ->
            policyService.create(1L, 2L, "POL", start, end, true)
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("aktif poliçe zaten var");
    }

    @Test
    void create_missing_customer_throws() {
        when(customerRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            policyService.create(1L, null, "POL", LocalDate.now(), LocalDate.now().plusDays(1), false)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Müşteri bulunamadı");
    }

    @Test
    void create_missing_car_when_carId_given_throws() {
        when(customerRepo.findById(1L)).thenReturn(Optional.of(cust));
        when(carRepo.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            policyService.create(1L, 9L, "POL", LocalDate.now(), LocalDate.now().plusDays(1), false)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Araç bulunamadı");
    }

    // ---------- get(...) ----------

    @Test
    void get_existing_returns_policy() {
        Policy p = new Policy();
        ReflectionTestUtils.setField(p, "id", 77L);
        when(policyRepo.findById(77L)).thenReturn(Optional.of(p));

        Policy found = policyService.get(77L);

        assertThat(found).isSameAs(p);
        verify(policyRepo).findById(77L);
    }

    @Test
    void get_missing_throws() {
        when(policyRepo.findById(66L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.get(66L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Poliçe bulunamadı");
    }

    // ---------- acceptFromQuote(...) ----------

    private Quote pendingQuote(LocalDate start, LocalDate end, boolean carActive) {
        Quote q = new Quote();
        q.setCustomer(cust);
        q.setCar(car);
        car.setActive(carActive);
        q.setProduct("KASKO");
        q.setCoverageStart(start);
        q.setCoverageEnd(end);
        q.setNetPremium(new BigDecimal("100.00"));
        q.setTax(new BigDecimal("10.00"));
        q.setGrossPremium(new BigDecimal("110.00"));
        q.setValidUntil(LocalDateTime.now().plusDays(1));
        q.setStatus(QuoteStatus.PENDING);
        ReflectionTestUtils.setField(q, "id", 5L);
        return q;
    }

    @Test
    void acceptFromQuote_happyPath_creates_active_policy_and_updates_quote() {
        var start = LocalDate.of(2025, 1, 1);
        var end   = LocalDate.of(2025, 12, 31);
        Quote q = pendingQuote(start, end, true);

        when(quoteRepo.findById(5L)).thenReturn(Optional.of(q));
        when(policyRepo.countActiveOverlaps(2L, start, end)).thenReturn(0L);

        when(policyRepo.save(any(Policy.class))).thenAnswer(inv -> {
            Policy p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 999L);
            return p;
        });

        Policy policy = policyService.acceptFromQuote(5L);

        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(policy.isActive()).isTrue();
        assertThat(policy.getCustomer()).isSameAs(cust);
        assertThat(policy.getCar()).isSameAs(car);
        assertThat(policy.getPremium()).isEqualByComparingTo("100.00");
        assertThat(policy.getTax()).isEqualByComparingTo("10.00");
        assertThat(policy.getTotal()).isEqualByComparingTo("110.00");

        // Quote güncellendi mi?
        verify(quoteRepo).save(argThat(saved ->
            saved.getStatus() == QuoteStatus.APPROVED &&
            saved.getAcceptedAt() != null
        ));
    }

    @Test
    void acceptFromQuote_not_pending_throws() {
        Quote q = pendingQuote(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), true);
        q.setStatus(QuoteStatus.APPROVED);
        when(quoteRepo.findById(5L)).thenReturn(Optional.of(q));

        assertThatThrownBy(() -> policyService.acceptFromQuote(5L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not pending");
    }

    @Test
    void acceptFromQuote_expired_throws() {
        Quote q = pendingQuote(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), true);
        q.setValidUntil(LocalDateTime.now().minusMinutes(1));
        when(quoteRepo.findById(5L)).thenReturn(Optional.of(q));

        assertThatThrownBy(() -> policyService.acceptFromQuote(5L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("expired");
    }

    @Test
    void acceptFromQuote_car_inactive_throws() {
        Quote q = pendingQuote(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), false);
        when(quoteRepo.findById(5L)).thenReturn(Optional.of(q));
        when(policyRepo.countActiveOverlaps(anyLong(), any(), any())).thenReturn(0L);

        assertThatThrownBy(() -> policyService.acceptFromQuote(5L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Araç pasif");
    }

    @Test
    void acceptFromQuote_overlap_throws() {
        var start = LocalDate.now().plusDays(1);
        var end   = start.plusDays(20);
        Quote q = pendingQuote(start, end, true);
        when(quoteRepo.findById(5L)).thenReturn(Optional.of(q));
        when(policyRepo.countActiveOverlaps(2L, start, end)).thenReturn(2L);

        assertThatThrownBy(() -> policyService.acceptFromQuote(5L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("aktif bir poliçe zaten var");
    }

    @Test
    void acceptFromQuote_quote_not_found_throws() {
        when(quoteRepo.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.acceptFromQuote(5L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Quote not found");
    }

    // ---------- countByCar / countActiveByCar / search ----------

    @Test
    void countByCar_delegates_to_repo() {
        when(policyRepo.countByCarId(2L)).thenReturn(7L);
        assertThat(policyService.countByCar(2L)).isEqualTo(7L);
        verify(policyRepo).countByCarId(2L);
    }

    @Test
    void countActiveByCar_delegates_to_repo() {
        when(policyRepo.countByCarIdAndActiveTrue(2L)).thenReturn(3L);
        assertThat(policyService.countActiveByCar(2L)).isEqualTo(3L);
        verify(policyRepo).countByCarIdAndActiveTrue(2L);
    }

    @Test
    void search_delegates_to_repo() {
        when(policyRepo.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        assertThat(policyService.search(null, null, null, null, null, null, null).getContent())
                .isEmpty();
        verify(policyRepo).search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
    }

    // ---------- activatePolicy / deletePolicy / cancelPolicy ----------

    @Test
    void activatePolicy_happyPath_sets_active_and_saves() {
        Policy p = new Policy();
        ReflectionTestUtils.setField(p, "id", 10L);
        p.setCustomer(cust);
        p.setCar(car);
        p.setStartDate(LocalDate.of(2025,1,1));
        p.setEndDate(LocalDate.of(2025,12,31));
        p.setActive(false);

        when(policyRepo.findById(10L)).thenReturn(Optional.of(p));
        when(policyRepo.countActiveOverlapsExcept(2L, p.getStartDate(), p.getEndDate(), 10L)).thenReturn(0L);

        policyService.activatePolicy(10L);

        assertThat(p.isActive()).isTrue();
        assertThat(p.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        verify(policyRepo).save(p);
    }

    @Test
    void activatePolicy_already_active_throws() {
        Policy p = new Policy();
        p.setCar(car);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(1));
        p.setActive(true);
        when(policyRepo.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> policyService.activatePolicy(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("zaten aktif");
    }

    @Test
    void activatePolicy_overlap_throws() {
        Policy p = new Policy();
        ReflectionTestUtils.setField(p, "id", 11L);
        p.setCar(car);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(1));
        p.setActive(false);
        when(policyRepo.findById(11L)).thenReturn(Optional.of(p));
        when(policyRepo.countActiveOverlapsExcept(2L, p.getStartDate(), p.getEndDate(), 11L)).thenReturn(2L);

        assertThatThrownBy(() -> policyService.activatePolicy(11L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Aktifleştirilemez");
    }

    @Test
    void deletePolicy_inactive_deletes() {
        Policy p = new Policy();
        p.setActive(false);
        when(policyRepo.findById(9L)).thenReturn(Optional.of(p));

        policyService.deletePolicy(9L);

        verify(policyRepo).deleteById(9L);
    }

    @Test
    void deletePolicy_active_throws() {
        Policy p = new Policy();
        p.setActive(true);
        when(policyRepo.findById(9L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> policyService.deletePolicy(9L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Aktif poliçe silinemez");
    }

    @Test
    void cancelPolicy_success_when_updated1() {
        when(policyRepo.deactivatePolicy(5L)).thenReturn(1);
        policyService.cancelPolicy(5L);
        verify(policyRepo).deactivatePolicy(5L);
    }

    @Test
    void cancelPolicy_throws_when_zero_updated() {
        when(policyRepo.deactivatePolicy(6L)).thenReturn(0);
        assertThatThrownBy(() -> policyService.cancelPolicy(6L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already inactive");
    }
}
