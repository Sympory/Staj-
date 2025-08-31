// src/main/java/com/example/staj/services/CustomerService.java
package com.example.staj.services;

import com.example.staj.entity.Customer;
import com.example.staj.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerService {
    private final CustomerRepository customerRepo;

    public CustomerService(CustomerRepository customerRepo) {
        this.customerRepo = customerRepo;
    }

    public List<Customer> list() { return customerRepo.findAll(); }

    public Customer create(Customer c) { return customerRepo.save(c); }

    public Customer update(Long id, String name, String email) {
        var c = customerRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Geçersiz müşteri ID: " + id));
        c.setName(name);
        c.setEmail(email);
        return customerRepo.save(c);
    }
    
    public Customer get(Long id) {
        return customerRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Müşteri bulunamadı: " + id));
    }


    public void delete(Long id) {
        if (!customerRepo.existsById(id))
            throw new IllegalArgumentException("Müşteri bulunamadı!");
        customerRepo.deleteById(id);
    }
}
