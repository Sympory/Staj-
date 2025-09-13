// // src/test/java/com/example/staj/repository/PolicyEntityJpaTest.java
// package com.example.staj.repository;

// import com.example.staj.entity.*;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
// import org.springframework.dao.DataIntegrityViolationException;

// import java.math.BigDecimal;
// import java.time.LocalDate;

// import static org.assertj.core.api.Assertions.*;

// @DataJpaTest
// class PolicyEntityJpaTest {

//     @Autowired PolicyRepository policyRepo;
//     @Autowired CustomerRepository customerRepo;
//     @Autowired CarRepository carRepo;
//     @Autowired BrandRepository brandRepo;
//     @Autowired CarModelRepository carModelRepo;

//     private Customer createCustomer(String name, String email) {
//         var c = new Customer();
//         c.setName(name);
//         c.setEmail(email);
//         return customerRepo.save(c);
//     }

//     private Brand createBrand(String name) {
//         var b = new Brand();
//         b.setName(name);
//         return brandRepo.save(b);
//     }

//     private CarModel createModel(String name, Brand brand) {
//         var m = new CarModel();
//         m.setName(name);
//         m.setBrand(brand);
//         return carModelRepo.save(m);
//     }

//     private Car createCar(Customer customer, Brand brand, CarModel model, String plate, int year) {
//         var car = new Car();
//         car.setCustomer(customer);
//         car.setBrand(brand);
//         car.setModel(model);
//         car.setPlate(plate);
//         car.setModelYear(year);
//         car.setActive(true);
//         return carRepo.save(car);
//     }

//     @Test
//     void policy_persist_ve_fetch_calismali() {
//         // Arrange: tüm ilişkileri kur
//         var cust = createCustomer("Ali", "ali@test.com");
//         var brand = createBrand("BMW");
//         var model = createModel("5 Serisi", brand);
//         var car   = createCar(cust, brand, model, "58 DR 801", 2018);

//         var start = LocalDate.now();
//         var end   = start.plusYears(1);

//         var p = new Policy();
//         p.setPolicyNumber("PN-0001");
//         p.setStartDate(start);
//         p.setEndDate(end);
//         p.setCustomer(cust);
//         p.setCar(car);
//         p.setPremium(new BigDecimal("1000.00"));
//         p.setTax(new BigDecimal("100.00"));
//         p.setTotal(new BigDecimal("1100.00"));

//         // Act: kaydet
//         var saved = policyRepo.saveAndFlush(p);

//         // Assert: geri oku/doğrula
//         assertThat(saved.getId()).isNotNull();
//         var found = policyRepo.findById(saved.getId()).orElseThrow();
//         assertThat(found.getPolicyNumber()).isEqualTo("PN-0001");
//         assertThat(found.getCustomer().getId()).isEqualTo(cust.getId());
//         assertThat(found.getCar().getId()).isEqualTo(car.getId());
//         assertThat(found.getPremium()).isEqualByComparingTo("1000.00");
//         assertThat(found.getTax()).isEqualByComparingTo("100.00");
//         assertThat(found.getTotal()).isEqualByComparingTo("1100.00");
//         assertThat(found.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
//         assertThat(found.isActive()).isTrue();
//     }

//     @Test
//     void policy_number_unique_kisitini_ihlal_edince_hata() {
//         var cust = createCustomer("Ayşe", "ayse@test.com");
//         var brand = createBrand("Ford");
//         var model = createModel("Fiesta", brand);
//         var car   = createCar(cust, brand, model, "22 AD 234", 2020);

//         var p1 = new Policy();
//         p1.setPolicyNumber("PN-UNIQ");
//         p1.setStartDate(LocalDate.now());
//         p1.setEndDate(LocalDate.now().plusYears(1));
//         p1.setCustomer(cust);
//         p1.setCar(car);
//         policyRepo.saveAndFlush(p1);

//         var p2 = new Policy();
//         p2.setPolicyNumber("PN-UNIQ"); // aynı numara
//         p2.setStartDate(LocalDate.now());
//         p2.setEndDate(LocalDate.now().plusYears(1));
//         p2.setCustomer(cust);
//         p2.setCar(car);

//         // Aynı policy_number için veri tabanı unique kısıtı patlamalı
//         assertThatThrownBy(() -> policyRepo.saveAndFlush(p2))
//                 .isInstanceOf(DataIntegrityViolationException.class);
//     }
// }
