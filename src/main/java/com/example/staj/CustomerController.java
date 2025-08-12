package com.example.staj;

import com.example.staj.repository.CustomerRepository;
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

    @Autowired
    public CustomerController(CustomerRepository customerRepo,
                              CarRepository carRepo,
                              PolicyRepository policyRepo) {
        this.customerRepo = customerRepo;
        this.carRepo = carRepo;
        this.policyRepo = policyRepo;
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
    return "add-car";
}

@PostMapping("/cars")
public String createCar(@RequestParam Long customerId,
                        @RequestParam String plate,
                        @RequestParam(required = false) String brand,
                        @RequestParam(required = false) String model,
                        @RequestParam(required = false) Integer modelYear,
                        RedirectAttributes ra) {
    var customer = customerRepo.findById(customerId).orElseThrow();
    var car = new Car();
    car.setCustomer(customer);
    car.setPlate(plate);
    car.setBrand(brand);
    car.setModel(model);
    car.setModelYear(modelYear);
    carRepo.save(car);
    ra.addFlashAttribute("msg", "Araba eklendi: " + plate);
    return "redirect:/policies/new"; // sonra poliçe eklemeye geri dön
}

    // Poliçe ekleme formu
    @GetMapping("/policies/new")
    public String showAddPolicyForm(Model model) {
        model.addAttribute("customers", customerRepo.findAll()); // <-- bean üzerinden
        model.addAttribute("cars", carRepo.findAll());           // <-- bean üzerinden
        return "add-policy";
    }
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
