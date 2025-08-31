package com.example.staj.controller;

import com.example.staj.entity.Car;
import com.example.staj.entity.CarModel;
import com.example.staj.entity.Customer;
import com.example.staj.services.CarService;
import com.example.staj.services.CustomerService;
import com.example.staj.services.PolicyService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CustomerController {

    private final CustomerService customerService;
    private final CarService carService;
    private final PolicyService policyService;

    @Autowired
    public CustomerController(CustomerService customerService,
                              CarService carService,
                              PolicyService policyService) {
        this.customerService = customerService;
        this.carService = carService;
        this.policyService = policyService;
    }

    // Ana sayfa
    @GetMapping("/")
    public String home() { return "index"; }

    // === Customer ===

    @GetMapping("/customers/new")
    public String showAddCustomerForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "add-customer";
    }

    @PostMapping("/customers")
    public String createCustomer(@Valid @ModelAttribute("customer") Customer customer,
                                 BindingResult binding,
                                 RedirectAttributes ra) {
        if (binding.hasErrors()) return "add-customer";
        customerService.create(customer);
        ra.addFlashAttribute("msg", "Müşteri eklendi: " + customer.getName());
        return "redirect:/customers";
    }

    @GetMapping("/customers")
    public String listCustomers(Model model) {
        model.addAttribute("customers", customerService.list());
        return "list-customers";
    }

    @GetMapping("/customers/delete/{id}")
    public String deleteCustomer(@PathVariable Long id, RedirectAttributes ra) {
        try {
            customerService.delete(id);
            ra.addFlashAttribute("msg", "Müşteri silindi (ID: " + id + ")");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customers";
    }

    @PostMapping("/customers/update/{id}")
    public String updateCustomer(@PathVariable Long id,
                                 @RequestParam String name,
                                 @RequestParam String email,
                                 RedirectAttributes ra) {
        customerService.update(id, name, email);
        ra.addFlashAttribute("msg", "Müşteri güncellendi: " + name);
        return "redirect:/customers";
    }

    // === Car (service ile) ===

    @GetMapping("/cars/new")
    public String showAddCarForm(Model model) {
        model.addAttribute("car", new Car());
        model.addAttribute("customers", customerService.list());
        model.addAttribute("cars", carService.listCars());
        model.addAttribute("brands", carService.listBrands());   // ✅ repo yok
        return "add-car";
    }

    @PostMapping("/cars")
    public String createCar(@RequestParam Long customerId,
                            @RequestParam String plate,
                            @RequestParam Long brandId,
                            @RequestParam Long modelId,
                            @RequestParam(required = false) Integer modelYear,
                            RedirectAttributes ra) {
        try {
            var car = carService.createCar(customerId, plate, brandId, modelId, modelYear);
            ra.addFlashAttribute("msg", "Araba eklendi: " + car.getPlate());
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("err", ex.getMessage());
        }
        return "redirect:/cars/new";
    }

    @PostMapping("/cars/{id}/update")
    public String updateCar(@PathVariable Long id,
                            @RequestParam Long customerId,
                            @RequestParam String plate,
                            @RequestParam Long brandId,
                            @RequestParam Long modelId,
                            @RequestParam(required = false) Integer modelYear,
                            RedirectAttributes ra) {
        try {
            var car = carService.updateCar(id, customerId, plate, brandId, modelId, modelYear);
            ra.addFlashAttribute("msg", "Araba güncellendi: " + car.getPlate());
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("err", ex.getMessage());
        }
        return "redirect:/cars/new";
    }

    @PostMapping("/cars/{id}/delete")
    public String deleteCar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            carService.deleteCar(id);
            ra.addFlashAttribute("msg", "Araba silindi (ID: " + id + ")");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("err", ex.getMessage());
        }
        return "redirect:/cars/new";
    }

    @GetMapping("/api/brands/{brandId}/models")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> findModelsByBrand(@PathVariable Long brandId) {
        var list = carService.listModelsByBrand(brandId).stream()
                .map((CarModel m) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", m.getId());
                    row.put("name", m.getName());
                    return row;
                })
                .toList();
        return ResponseEntity.ok(list);
    }

    // === Policy (tamamen service) ===

    @GetMapping("/policies")
    public String listPolicies(@RequestParam(required = false) String q,
                               @RequestParam(required = false) Boolean active,
                               @RequestParam(required = false) Long customerId,
                               @RequestParam(required = false) Long carId,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startFrom,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endTo,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {

        var pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                org.springframework.data.domain.Sort.by("endDate").descending()
        );

        var result = policyService.search(q, active, customerId, carId, startFrom, endTo, pageable);

        model.addAttribute("page", result);
        model.addAttribute("policies", result.getContent());
        model.addAttribute("q", q);
        model.addAttribute("active", active);
        model.addAttribute("customerId", customerId);
        model.addAttribute("carId", carId);
        model.addAttribute("startFrom", startFrom);
        model.addAttribute("endTo", endTo);
        model.addAttribute("customers", customerService.list());
        model.addAttribute("cars", carService.listCars());

        return "list-policies";
    }

    @PostMapping("/policies/{id}/activate")
    public String activatePolicy(@PathVariable Long id, RedirectAttributes ra) {
        try {
            policyService.activatePolicy(id);
            ra.addFlashAttribute("msg", "Poliçe aktifleştirildi: " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/policies";
    }

    @PostMapping("/policies/{id}/cancel")
    public String cancelPolicy(@PathVariable Long id, RedirectAttributes ra) {
        try {
            policyService.cancelPolicy(id);
            ra.addFlashAttribute("msg", "Poliçe iptal edildi: " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/policies";
    }

    @PostMapping("/policies/{id}/delete")
    public String deletePolicy(@PathVariable Long id, RedirectAttributes ra) {
        try {
            policyService.deletePolicy(id);
            ra.addFlashAttribute("msg", "Poliçe silindi (ID: " + id + ")");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/policies";
    }

    @PostMapping("/policies")
    public String createPolicy(@RequestParam Long customerId,
                               @RequestParam(required = false) Long carId,
                               @RequestParam String policyNumber,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                               @RequestParam(defaultValue = "false") boolean active,
                               RedirectAttributes ra) {
        try {
            Long id = policyService.create(customerId, carId, policyNumber, startDate, endDate, active);
            ra.addFlashAttribute("msg", "Poliçe oluşturuldu (ID: " + id + ")");
        } catch (Exception e) {
            ra.addFlashAttribute("err", e.getMessage());
        }
        return "redirect:/policies/new";
    }
}
