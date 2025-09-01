// src/main/java/com/example/staj/services/CarService.java
package com.example.staj.services;

import com.example.staj.entity.*;
import com.example.staj.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class CarService {
    private final CarRepository carRepo;
    private final CustomerRepository customerRepo;
    private final BrandRepository brandRepo;
    private final CarModelRepository carModelRepo;
    private final PolicyRepository policyRepo; // <-- eklendi
    private final QuoteRepository quoteRepo; // <-- eklendi for quoteRepo
    public CarService(CarRepository carRepo,
                      CustomerRepository customerRepo,
                      BrandRepository brandRepo,
                      CarModelRepository carModelRepo,
                      PolicyRepository policyRepo,
                      QuoteRepository quoteRepo) { // <-- added QuoteRepository
        this.carRepo = carRepo;
        this.customerRepo = customerRepo;
        this.brandRepo = brandRepo;
        this.carModelRepo = carModelRepo;
        this.policyRepo = policyRepo; // <-- eklendi
        this.quoteRepo = quoteRepo; // <-- assign quoteRepo
    }

    // --- READS ---
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

        if (!model.getBrand().getId().equals(brandId)) {
            throw new IllegalArgumentException(
                "Seçilen model (" + model.getName() + ") belirtilen markaya ait değil: " + brand.getName()
            );
        }


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

    // --- SEARCH ---
    public Page<Car> search(String q, Long customerId, Long brandId, Long modelId,
                            Integer yearFrom, Integer yearTo, Pageable pageable) {
        return carRepo.search(
            (q == null || q.isBlank()) ? null : q.trim(),
            customerId, brandId, modelId, yearFrom, yearTo, pageable
        );
    }

    // --- UPDATE ---
    @Transactional
    public Car updateCar(Long id, Long customerId, String plate, Long brandId, Long modelId, Integer modelYear) {
        var car = carRepo.findById(id).orElseThrow();
        var customer = customerRepo.findById(customerId).orElseThrow();
        var brand = brandRepo.findById(brandId).orElseThrow();
        var model = carModelRepo.findById(modelId).orElseThrow();

        var normalizedPlate = normalizePlate(plate);


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

    // --- ARAÇ PASİFE AL ---
    @Transactional
    public void deactivateCar(Long carId) {
        var car = carRepo.findById(carId).orElseThrow();
        if (!car.isActive()) return;

        car.setActive(false);
        carRepo.save(car);

        // Bağlı poliçeleri de pasifleştir (DRAFT, PENDING_APPROVAL, ACTIVE)
        var toClose = List.of(PolicyStatus.DRAFT, PolicyStatus.PENDING_APPROVAL, PolicyStatus.ACTIVE);
        policyRepo.passivateAllByCar(carId, PolicyStatus.PASSIVE, toClose);
    }

    // --- ARAÇ AKTİF ET ---
    @Transactional
    public void activateCar(Long carId) {
        var car = carRepo.findById(carId).orElseThrow();
        if (car.isActive()) return;
        car.setActive(true);
        carRepo.save(car);
        // Not: poliçeleri otomatik ACTIVE yapmıyoruz.
    }

@Transactional
public void deleteCar(Long id) {
    var car = carRepo.findById(id).orElseThrow();

    // 1) Aktif poliçe varsa: pasife çek ve SILME
    long activeCount = policyRepo.countByCarIdAndActiveTrue(id);
    if (activeCount > 0) {
        deactivateCar(id); // aktifleri PASSIVE + active=false yapar
        return;
    }

    // 2) Aktif yoksa: pasif poliçeleri temizle
    policyRepo.deleteAllPassiveByCarId(id);

    // 3) Bu araca bağlı TÜM teklifleri sil
    quoteRepo.deleteAllByCarId(id);

    // 4) Arabayı güvenle sil
    carRepo.delete(car);
}


    // --- helpers ---
    private String normalizePlate(String plate) {
        return plate == null ? null : plate.trim().toUpperCase();
    }
}
