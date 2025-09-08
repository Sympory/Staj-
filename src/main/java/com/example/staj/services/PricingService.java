package com.example.staj.services;

import com.example.staj.entity.Car;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class PricingService {
  public record Pricing(BigDecimal net, BigDecimal tax, BigDecimal gross) {}

  public Pricing price(Car car, String product, LocalDate start, LocalDate end) {
    if (start.isAfter(end)) throw new IllegalArgumentException("Başlangıç > Bitiş olamaz");

    long days = Math.max(ChronoUnit.DAYS.between(start, end), 1);

    String p = (product == null ? "GENEL" : product).toUpperCase();
    BigDecimal base = switch (p) {
      case "KASKO"  -> BigDecimal.valueOf(4000);
      case "TRAFIK" -> BigDecimal.valueOf(2000);
      default       -> BigDecimal.valueOf(3000);
    };

Integer year = car.getModelYear();
BigDecimal riskFactor = ( year <= 2015) ? BigDecimal.valueOf(1.2) : BigDecimal.ONE;

    BigDecimal net   = base.multiply(riskFactor)
                           .multiply(BigDecimal.valueOf(days))
                           .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);

    BigDecimal tax   = net.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal gross = net.add(tax).setScale(2, RoundingMode.HALF_UP);

    return new Pricing(net, tax, gross);
  }
}
