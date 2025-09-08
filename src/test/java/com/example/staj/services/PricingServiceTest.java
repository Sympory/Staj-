package com.example.staj.services;

import com.example.staj.entity.Car;
import com.example.staj.entity.Brand;
import com.example.staj.entity.CarModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    PricingService pricing = new PricingService();

    @Test
    void kasko_2015ten_eski_aracta_risk_faktoru_uygulanir() {
        Car car = new Car();
        car.setModelYear(2010);
        car.setBrand(new Brand());
        car.setModel(new CarModel());

        var start = LocalDate.of(2025, 8, 20);
        var end   = LocalDate.of(2026, 8, 20);

        var p = pricing.price(car, "KASKO", start, end);

        // Net = 4000 * 1.2 ≈ 4800 (yaklaşık, gün bazlı bölme var)
        assertThat(p.net()).isGreaterThan(new BigDecimal("4700"));
        assertThat(p.gross()).isEqualTo(p.net().add(p.tax())); // vergiler toplanmış mı
    }

    @Test
    void trafik_2016_ve_sonrasi_aracta_risk_faktoru_1_0() {
        Car car = new Car();
        car.setModelYear(2018);

        var p = pricing.price(car, "TRAFIK",
                LocalDate.of(2025, 8, 20),
                LocalDate.of(2026, 8, 20));

        assertThat(p.net()).isBetween(new BigDecimal("1990"), new BigDecimal("2010"));
    }
}
