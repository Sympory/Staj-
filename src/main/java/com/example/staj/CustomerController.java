package com.example.staj;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomerController {

    @GetMapping("/")
    public String home() {
        return "index"; // templates/index.html
    }

    @GetMapping("/customers/new")
    public String showAddCustomerForm() {
        return "add-customer"; // templates/add-customer.html
    }

    @GetMapping("/customers")
    public String listCustomers() {
        return "list-customers"; // templates/list-customers.html
    }

    @GetMapping("/customers/delete")
    public String deleteCustomerForm() {
        return "delete-customer"; // templates/delete-customer.html
    }
}
