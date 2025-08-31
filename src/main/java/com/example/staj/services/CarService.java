// src/main/java/com/example/staj/services/CarService.java
package com.example.staj.services;

import com.example.staj.entity.*;
import com.example.staj.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CarService {
    private final CarRepository carRepo;
    private final CustomerRepository customerRepo;
    private final BrandRepository brandRepo;
    private final CarModelRepository carModelRepo;

    public CarService(CarRepository carRepo,
                      CustomerRepository customerRepo,
                      BrandRepository brandRepo,
                      CarModelRepository carModelRepo) {
        this.carRepo = carRepo;
        this.customerRepo = customerRepo;
        this.brandRepo = brandRepo;
        this.carModelRepo = carModelRepo;
    }

    // --- READS (controller sadece service'ten ister) ---
    public List<Customer> listCustomers() { return customerRepo.findAll(); }
    public List<Car> listCars() { return carRepo.findAll(); }
    public List<Brand> listBrands() { return brandRepo.findAll(); }
    public List<CarModel> listModelsByBrand(Long brandId) { return carModelRepo.findByBrand_IdOrderByNameAsc(brandId); }

    // --- CREATE ---
    @Transactional
    public Car createCar(Long customerId, String plate, Long brandId, Long modelId, Integer modelYear) {
        var customer = customerRepo.findById(customerId).orElseThrow();
        var brand = brandRepo.findById(brandId).orElseThrow();
        var model = carModelRepo.findById(modelId).orElseThrow();

        var normalizedPlate = normalizePlate(plate);
        
        // ✅ Marka–Model tutarlılığı
    if (!model.getBrand().getId().equals(brandId)) {
        throw new IllegalArgumentException(
            "Seçilen model (" + model.getName() + ") belirtilen markaya ait değil: " + brand.getName()
        );
    }

        // plaka tekil olsun (örnek iş kuralı)
        carRepo.findByPlate(normalizedPlate).ifPresent(c -> {
            throw new IllegalArgumentException("Bu plaka zaten kayıtlı: " + normalizedPlate);
        });

        var car = new Car();
        car.setCustomer(customer);
        car.setPlate(normalizedPlate);
        car.setBrand(brand);
        car.setModel(model);
        car.setModelYear(modelYear);
        return carRepo.save(car);
    }

    // --- UPDATE ---
    @Transactional
    public Car updateCar(Long id, Long customerId, String plate, Long brandId, Long modelId, Integer modelYear) {
        var car = carRepo.findById(id).orElseThrow();
        var customer = customerRepo.findById(customerId).orElseThrow();
        var brand = brandRepo.findById(brandId).orElseThrow();
        var model = carModelRepo.findById(modelId).orElseThrow();

        var normalizedPlate = normalizePlate(plate);

        // aynı plaka başka arabada var mı?
        carRepo.findByPlate(normalizedPlate).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new IllegalArgumentException("Bu plaka başka bir araçta kullanılıyor: " + normalizedPlate);
            }
        });

        car.setCustomer(customer);
        car.setPlate(normalizedPlate);
        car.setBrand(brand);
        car.setModel(model);
        car.setModelYear(modelYear);
        return carRepo.save(car);
    }

    // --- DELETE ya da SOFT-DELETE ---
    @Transactional
    public void deleteCar(Long id) {
        // Eğer Car'ta active alanı eklersen (boolean active), burada soft-delete yap:
        var car = carRepo.findById(id).orElseThrow();
        car.setActive(false);
        carRepo.save(car);

        // Şimdilik hard delete:
        carRepo.deleteById(id);
    }

    private String normalizePlate(String plate) {
        return plate == null ? null : plate.trim().toUpperCase();
    }
}
