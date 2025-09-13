package com.example.staj.services;

import com.example.staj.entity.Car;
import com.example.staj.entity.Customer;

import org.springframework.stereotype.Service;
import com.example.staj.entity.enums.*;
import com.example.staj.services.PricingService.Pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;


import java.util.Map;

@Service
public class PricingService {
  public record Pricing(BigDecimal net, BigDecimal tax, BigDecimal gross) {
 
  // === Basit katsayı tabloları ===
  private static final Map<Color, Double> COLOR = Map.of(
      Color.RED, 1.05, Color.BLACK, 1.02, Color.WHITE, 1.00,
      Color.BLUE, 1.00, Color.GREY, 1.00, Color.OTHER, 1.01
  );
  private static final Map<FuelType, Double> FUEL = Map.of(
      FuelType.ELECTRIC, 0.97, FuelType.HYBRID, 0.98,
      FuelType.GASOLINE, 1.00, FuelType.DIESEL, 1.00
  );
  private static final Map<BodyType, Double> BODY = Map.of(
      BodyType.SUV, 1.03, BodyType.PICKUP, 1.04, BodyType.SEDAN, 1.00,
      BodyType.HATCHBACK, 0.99, BodyType.COUPE, 1.02, BodyType.VAN, 1.03,
      BodyType.OTHER, 1.00
  );
  private static final Map<Gender, Double> GENDER = Map.of(
      Gender.MALE, 1.02, Gender.FEMALE, 1.00
  );
  private static final double CLAIMS_LOADING = 1.10; // hasar varsa +%10
  private static final double TAX_RATE = 0.10;


  // Yeni imza: hasPreviousClaims + gender/color/fuel/bodytype dikkate alınır
  public Pricing price(Car car,
                       Customer customer,
                       Boolean hasPreviousClaims,
                       String product,
                       LocalDate start,
                       LocalDate end) {

    long days = Math.max(ChronoUnit.DAYS.between(start, end), 1);

    // Ürün bazlı basit bir base (istersen config’ten okuyabiliriz)
    BigDecimal base = switch (product.toUpperCase()) {
      case "KASKO"  -> BigDecimal.valueOf(4000);
      case "TRAFIK" -> BigDecimal.valueOf(2000);
      default       -> BigDecimal.valueOf(3000);
    };

    // Model yılı örnek kuralı koruyorum (eski kodun)
    double factor = (car.getModelYear() != null && car.getModelYear() <= 2015) ? 1.20 : 1.00;

    // Yeni faktörler (null ise 1.0)
    factor *= COLOR.getOrDefault(nz(car.getColor()), 1.00);
    factor *= FUEL.getOrDefault(nz(car.getFuelType()), 1.00);
    factor *= BODY.getOrDefault(nz(car.getBodyType()), 1.00);

    if (customer != null && customer.getGender() != null) {
      factor *= GENDER.getOrDefault(customer.getGender(), 1.00);
    }

    if (Boolean.TRUE.equals(hasPreviousClaims)) {
      factor *= CLAIMS_LOADING;
    }

    BigDecimal net = base
        .multiply(BigDecimal.valueOf(factor))
        .multiply(BigDecimal.valueOf(days).divide(BigDecimal.valueOf(365), 6, RoundingMode.HALF_UP))
        .setScale(2, RoundingMode.HALF_UP);

    BigDecimal tax   = net.multiply(BigDecimal.valueOf(TAX_RATE)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal gross = net.add(tax);

    return new Pricing(net, tax, gross);
  }

  // null -> OTHER yardımcıları
  private static Color nz(Color c) { return c == null ? Color.OTHER : c; }
  private static FuelType nz(FuelType f) { return f == null ? FuelType.GASOLINE : f; }
  private static BodyType nz(BodyType b) { return b == null ? BodyType.OTHER : b; }
}

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
