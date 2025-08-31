package com.example.staj.controller;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.staj.repository.BrandRepository;
import com.example.staj.repository.CarModelRepository;
import com.example.staj.entity.Brand;
import com.example.staj.entity.CarModel;
import org.springframework.http.ResponseEntity;
import java.util.stream.Collectors;
import com.example.staj.repository.CustomerRepository;
import com.example.staj.entity.Car;
import com.example.staj.entity.Customer;
import com.example.staj.entity.Policy;
import com.example.staj.repository.CarRepository;
import com.example.staj.repository.PolicyRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class CustomerController {

    private final CustomerRepository customerRepo;
    private final CarRepository carRepo;
    private final PolicyRepository policyRepo;
    private final CarModelRepository carModelRepo;
    private final BrandRepository brandRepo;


@Autowired
public CustomerController(CustomerRepository customerRepo,
                          CarRepository carRepo,
                          PolicyRepository policyRepo,
                          BrandRepository brandRepo,
                          CarModelRepository carModelRepo) {
    this.customerRepo = customerRepo;
    this.carRepo = carRepo;
    this.policyRepo = policyRepo;
    this.brandRepo = brandRepo;
    this.carModelRepo = carModelRepo;
}


    // Ana sayfa
    @GetMapping("/")
    public String home() { return "index"; }

    // Müşteri ekleme formu
    @GetMapping("/customers/new")
    public String showAddCustomerForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "add-customer";
    }

    // Müşteri kaydet
    @PostMapping("/customers")
    public String createCustomer(@Valid @ModelAttribute("customer") Customer customer,
                                 BindingResult binding,
                                 RedirectAttributes ra) {
        if (binding.hasErrors()) return "add-customer";
        customerRepo.save(customer);
        ra.addFlashAttribute("msg", "Müşteri eklendi: " + customer.getName());
        return "redirect:/customers";
    }

    // Liste
    @GetMapping("/customers")
    public String listCustomers(Model model) {
        model.addAttribute("customers", customerRepo.findAll());
        return "list-customers";
    }

    // Sil
    @GetMapping("/customers/delete/{id}")
    public String deleteCustomer(@PathVariable Long id, RedirectAttributes ra) {
        if (customerRepo.existsById(id)) {
            customerRepo.deleteById(id);
            ra.addFlashAttribute("msg", "Müşteri silindi (ID: " + id + ")");
        } else {
            ra.addFlashAttribute("error", "Müşteri bulunamadı!");
        }
        return "redirect:/customers";
    }

    // Satır içi güncelle
    @PostMapping("/customers/update/{id}")
    public String updateCustomer(@PathVariable Long id,
                                 @RequestParam String name,
                                 @RequestParam String email,
                                 RedirectAttributes ra) {
        var c = customerRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Geçersiz müşteri ID: " + id));
        c.setName(name);
        c.setEmail(email);
        customerRepo.save(c);
        ra.addFlashAttribute("msg", "Müşteri güncellendi: " + name);
        return "redirect:/customers";
    }

    // === Poliçe ===

    // imports: CarRepository, CustomerRepository, RedirectAttributes, etc.


    @GetMapping("/cars/new")
    public String showAddCarForm(Model model) {
        model.addAttribute("car", new Car());
        model.addAttribute("customers", customerRepo.findAll());
        model.addAttribute("cars", carRepo.findAllByActiveTrue());  // ✅ sadece aktif
        model.addAttribute("brands", brandRepo.findAll()); // ✅ Marka listesini sayfaya gönder
        return "add-car";
    }


 @PostMapping("/cars")
    public String createCar(@RequestParam Long customerId,
                            @RequestParam String plate,
                            @RequestParam Long brandId,   // ✅ id olarak al
                            @RequestParam Long modelId,   // ✅ id olarak al
                            @RequestParam(required = false) Integer modelYear,
                            RedirectAttributes ra) {

        var customer = customerRepo.findById(customerId).orElseThrow();
        var brandEntity = brandRepo.findById(brandId).orElseThrow();
        var modelEntity = carModelRepo.findById(modelId).orElseThrow();
                            
        var car = new Car();
     
    String normPlate = plate == null ? null
            : plate.trim().toUpperCase().replaceAll("\\s+", " ");

    if (carRepo.existsByPlateIgnoreCase(normPlate)) {
        ra.addFlashAttribute("err", "Bu plaka zaten kayıtlı: " + normPlate);
        return "redirect:/cars/new";
    }

        car.setCustomer(customer);
        car.setPlate(plate != null ? plate.trim().toUpperCase() : null);
        car.setBrand(brandEntity);   // ✅ entity set
        car.setModel(modelEntity);   // ✅ entity set
        car.setModelYear(modelYear);

        carRepo.save(car);
        ra.addFlashAttribute("msg", "Araba eklendi: " + car.getPlate());
        return "redirect:/cars/new";
    }
    

 // Araba GÜNCELLE (satır-içi)
    @PostMapping("/cars/{id}/update")
    public String updateCar(@PathVariable Long id,
                            @RequestParam Long customerId,
                            @RequestParam String plate,
                            @RequestParam Long brandId,   // ✅ id olarak al
                            @RequestParam Long modelId,   // ✅ id olarak al
                            @RequestParam(required = false) Integer modelYear,
                            RedirectAttributes ra) {

        var car = carRepo.findById(id).orElseThrow();
        var customer = customerRepo.findById(customerId).orElseThrow();
        var brandEntity = brandRepo.findById(brandId).orElseThrow();
        var modelEntity = carModelRepo.findById(modelId).orElseThrow();

        if(carRepo.existsByPlateIgnoreCaseAndIdNot(plate.trim().toUpperCase(), id)) {
            ra.addFlashAttribute("err", "Bu plaka zaten kayıtlı: " + plate);
            return "redirect:/cars/new";
        }
        car.setCustomer(customer);
        car.setPlate(plate != null ? plate.trim().toUpperCase() : null);
        car.setBrand(brandEntity);   // ✅
        car.setModel(modelEntity);   // ✅
        car.setModelYear(modelYear);
                              
        carRepo.save(car);
        ra.addFlashAttribute("msg", "Araba güncellendi: " + car.getPlate());
        return "redirect:/cars/new";
    }

@GetMapping("/api/brands/{brandId}/models")
@ResponseBody
public ResponseEntity<List<Map<String, Object>>> findModelsByBrand(@PathVariable Long brandId) {
    List<CarModel> models = carModelRepo.findByBrand_IdOrderByNameAsc(brandId);

    List<Map<String, Object>> list = models.stream()
            .map((CarModel m) -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", m.getId());      
                row.put("name", m.getName());  
                return row;
            })
            .collect(Collectors.toList());     
    return ResponseEntity.ok(list);
}



// Araba sil
@PostMapping("/cars/{id}/delete")
public String deleteCar(@PathVariable Long id, RedirectAttributes ra) {
    var car = carRepo.findById(id).orElse(null);
    if (car == null) {
        ra.addFlashAttribute("err", "Araba bulunamadı!");
        return "redirect:/cars/new";
    }
    // ✅ Soft delete
    car.setActive(false);
    carRepo.save(car);
    ra.addFlashAttribute("msg", "Araba pasife alındı: " + car.getPlate());
    return "redirect:/cars/new";
}


    // Poliçe aktifleştir
@PostMapping("/policies/{id}/activate")
public String activatePolicy(@PathVariable Long id, RedirectAttributes ra) {
    var p = policyRepo.findById(id).orElse(null);
    if (p == null) {
        ra.addFlashAttribute("err", "Poliçe bulunamadı!");
    } else {
        p.setActive(true);
        policyRepo.save(p);
        ra.addFlashAttribute("msg", "Poliçe aktifleştirildi: " + p.getPolicyNumber());
    }
    return "redirect:/policies";
}
// ✅ Poliçeyi silme
    @PostMapping("/policies/{id}/delete")
    public String deletePolicy(@PathVariable Long id, RedirectAttributes ra) {
        if (policyRepo.existsById(id)) {
            policyRepo.deleteById(id);
            ra.addFlashAttribute("msg", "Poliçe silindi (ID: " + id + ")");
        } else {
            ra.addFlashAttribute("err", "Poliçe bulunamadı!");
        }
        return "redirect:/policies";
    }


    // Poliçe ekleme formu
    
    @GetMapping("/policies")
public String listPolicies(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Long customerId,
        @RequestParam(required = false) Long carId,
        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startFrom,
        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        Model model
) {
    var pageable = org.springframework.data.domain.PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), 100), // 1..100
            org.springframework.data.domain.Sort.by("endDate").descending()
    );

    var result = policyRepo.search(
            (q == null || q.isBlank()) ? null : q.trim(),
            active, customerId, carId, startFrom, endTo, pageable
    );

    model.addAttribute("page", result);
    model.addAttribute("policies", result.getContent());
    model.addAttribute("q", q);
    model.addAttribute("active", active);
    model.addAttribute("customerId", customerId);
    model.addAttribute("carId", carId);
    model.addAttribute("startFrom", startFrom);
    model.addAttribute("endTo", endTo);

    // filtre dropdown'ları için
    model.addAttribute("customers", customerRepo.findAll());
    model.addAttribute("cars", carRepo.findAll());
    return "list-policies";
}
    // Poliçe iptal et
@PostMapping("/policies/{id}/cancel")
public String cancelPolicy(@PathVariable Long id, RedirectAttributes ra) {
    var p = policyRepo.findById(id).orElse(null);
    if (p == null) {
        ra.addFlashAttribute("err", "Poliçe bulunamadı!");
    } else {
        p.setActive(false); // aktiflik kaldır
        // Eğer Policy entity'nde status alanı varsa:
        // p.setStatus(PolicyStatus.CANCELLED);
        policyRepo.save(p);
        ra.addFlashAttribute("msg", "Poliçe iptal edildi: " + p.getPolicyNumber());
    }
    return "redirect:/policies";
}

    // Poliçe kaydet
    @PostMapping("/policies")
    public String createPolicy(@RequestParam Long customerId,
                               @RequestParam Long carId,
                               @RequestParam String policyNumber,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                               @RequestParam(required = false, defaultValue = "false") boolean active,
                               RedirectAttributes ra) {
        var customer = customerRepo.findById(customerId).orElseThrow();
        var car = carRepo.findById(carId).orElseThrow();

        var p = new Policy();
        p.setPolicyNumber(policyNumber);
        p.setStartDate(startDate);
        p.setEndDate(endDate);
        p.setActive(active);
        p.setCustomer(customer);
        p.setCar(car);
        policyRepo.save(p);

        ra.addFlashAttribute("msg", "Poliçe oluşturuldu: " + policyNumber);
        return "redirect:/policies/new"; // istersen /customers'a döndür
    }
}
