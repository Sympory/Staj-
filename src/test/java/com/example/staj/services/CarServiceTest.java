package com.example.staj.services;

import com.example.staj.entity.*;
import com.example.staj.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock CarRepository carRepo;
    @Mock CustomerRepository customerRepo;
    @Mock BrandRepository brandRepo;
    @Mock CarModelRepository carModelRepo;
    @Mock PolicyRepository policyRepo;
    @Mock QuoteRepository quoteRepo;
    @Mock PricingService pricing;

    @InjectMocks CarService carService;

    private Car stubCar(long id, int modelYear, Brand brand, CarModel model, boolean active) {
        Car c = new Car();
        c.setId(id);
        c.setModelYear(modelYear);
        c.setBrand(brand);
        c.setModel(model);
        c.setActive(active);
        return c;
    }

    private Brand stubBrand(long id, String name) {
        Brand b = new Brand();
        b.setId(id);
        b.setName(name);
        return b;
    }

    private CarModel stubModel(long id, String name, Brand brand) {
        CarModel m = new CarModel();
        m.setId(id);
        m.setName(name);
        m.setBrand(brand);
        return m;
    }

    private Customer stubCustomer(long id, String name) {
        Customer cu = new Customer();
        cu.setId(id);
        cu.setName(name);
        return cu;
    }

    @Test
    void updateCar_modelYili_degistiginde_pending_teklifler_reprice_edilir() {
        // Arrange
        Brand bmw = stubBrand(2L, "BMW");
        CarModel m5 = stubModel(4L, "5 Serisi", bmw);

        Car existing = stubCar(10L, 2015, bmw, m5, true);
        when(carRepo.findById(10L)).thenReturn(Optional.of(existing));
        when(customerRepo.findById(11L)).thenReturn(Optional.of(stubCustomer(11L, "X")));
        when(brandRepo.findById(2L)).thenReturn(Optional.of(bmw));
        when(carModelRepo.findById(4L)).thenReturn(Optional.of(m5));

        // plakayı aynı arabada bulursak sorun yok
        when(carRepo.findByPlate("58 DR 801")).thenReturn(Optional.of(existing));
        when(carRepo.save(any(Car.class))).thenAnswer(inv -> inv.getArgument(0));

        // Bu araca ait 1 adet PENDING teklif
        Quote q1 = new Quote();
        q1.setProduct("KASKO");
        q1.setCoverageStart(LocalDate.now());
        q1.setCoverageEnd(LocalDate.now().plusDays(30));
        q1.setStatus(QuoteStatus.PENDING);
        when(quoteRepo.findAllByCarIdAndStatus(10L, QuoteStatus.PENDING))
                .thenReturn(List.of(q1));

        // Pricing yanıtını sabitle
        when(pricing.price(any(Car.class), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new PricingService.Pricing(
                        new BigDecimal("123.45"),
                        new BigDecimal("12.34"),
                        new BigDecimal("135.79")
                ));

        // Act
        Car updated = carService.updateCar(
                10L,        // id
                11L,        // customerId
                "58 DR 801",
                2L,         // brandId
                4L,         // modelId
                2026        // modelYear (DEĞİŞTİ!)
        );

      ArgumentCaptor<List<Quote>> captor = ArgumentCaptor.forClass(List.class);
        verify(quoteRepo).saveAll(captor.capture());
        var saved = captor.getValue();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getGrossPremium()).isEqualByComparingTo("135.79");
        assertThat(updated.getModelYear()).isEqualTo(2026);

    }

    @Test
    void deleteCar_aktif_policeler_varsa_silmez_pasife_ceker() {
        // Arrange
        Brand bmw = stubBrand(2L, "BMW");
        CarModel m5 = stubModel(4L, "5 Serisi", bmw);
        Car car = stubCar(10L, 2015, bmw, m5, true);

        when(carRepo.findById(10L)).thenReturn(Optional.of(car));
        when(policyRepo.countByCarIdAndActiveTrue(10L)).thenReturn(1L);

        // Act
        carService.deleteCar(10L);

        // Assert
        verify(policyRepo).passivateAllByCar(eq(10L),
                eq(PolicyStatus.PASSIVE),
                argThat(list -> list.containsAll(List.of(PolicyStatus.DRAFT, PolicyStatus.PENDING_APPROVAL, PolicyStatus.ACTIVE))));
        verify(carRepo, never()).delete(any());
        verify(policyRepo, never()).deleteAllPassiveByCarId(anyLong());
        verify(quoteRepo, never()).deleteAllByCarId(anyLong());
    }

    @Test
    void deleteCar_aktif_poliçe_yoksa_pasifleri_temizler_teklifleri_siler_ve_araci_siler() {
        // Arrange
        Brand bmw = stubBrand(2L, "BMW");
        CarModel m5 = stubModel(4L, "5 Serisi", bmw);
        Car car = stubCar(10L, 2015, bmw, m5, false);

        when(carRepo.findById(10L)).thenReturn(Optional.of(car));
        when(policyRepo.countByCarIdAndActiveTrue(10L)).thenReturn(0L);

        // Act
        carService.deleteCar(10L);

        // Assert
        verify(policyRepo).deleteAllPassiveByCarId(10L);
        verify(quoteRepo).deleteAllByCarId(10L);
        verify(carRepo).delete(car);
        verify(policyRepo, never()).passivateAllByCar(anyLong(), any(), any());
    }

    @Test
    void createCar_model_markaya_ait_degilse_hata() {
        // Arrange
        Brand bmw = stubBrand(2L, "BMW");
        Brand ford = stubBrand(3L, "Ford");
        CarModel fiesta = stubModel(7L, "Fiesta", ford); // Ford'un modeli

        when(customerRepo.findById(11L)).thenReturn(Optional.of(stubCustomer(11L, "X")));
        when(brandRepo.findById(2L)).thenReturn(Optional.of(bmw));
        when(carModelRepo.findById(7L)).thenReturn(Optional.of(fiesta));

        // Act & Assert
        assertThatThrownBy(() ->
                carService.createCar(11L, "58 DR 801", 2L, 7L, 2026) // 2(BMW) + 7(Ford modeli) => HATA
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("belirtilen markaya ait değil");
    }
}
