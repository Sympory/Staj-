package com.example.staj;

import com.example.staj.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerController {

    private final CustomerRepository repo;

    @Autowired
    public CustomerController(CustomerRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/customers/new")
    public String showAddCustomerForm() {
        return "add-customer";
    }

    @PostMapping("/customers")
    public String createCustomer(@RequestParam String name,
                                 @RequestParam String email,
                                 RedirectAttributes ra) {
        Customer c = new Customer();
        c.setName(name);
        c.setEmail(email);
        repo.save(c);
        ra.addFlashAttribute("msg", "Müşteri eklendi: " + name);
        return "redirect:/customers";
    }

    @GetMapping("/customers")
    public String listCustomers(Model model) {
        model.addAttribute("customers", repo.findAll());
        return "list-customers";
    }

    @GetMapping("/customers/delete/{id}")
    public String deleteCustomer(@PathVariable Long id, RedirectAttributes ra) {
        if (repo.existsById(id)) {
            repo.deleteById(id);
            ra.addFlashAttribute("msg", "Müşteri silindi (ID: " + id + ")");
        } else {
            ra.addFlashAttribute("error", "Müşteri bulunamadı!");
        }
        return "redirect:/customers";
    }

    @PostMapping("/customers/update/{id}")
    public String updateCustomer(@PathVariable Long id,
                                 @RequestParam String name,
                                 @RequestParam String email,
                                 RedirectAttributes ra) {
        Customer customer = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Geçersiz müşteri ID: " + id));

        customer.setName(name);
        customer.setEmail(email);
        repo.save(customer);

        ra.addFlashAttribute("msg", "Müşteri güncellendi: " + name);
        return "redirect:/customers";
    }
}
