package com.example.staj.services;

import java.util.List;

import com.example.staj.entity.Customer;

public interface CustomerService {
    List<Customer> getAllCustomers();
    Customer getCustomerById(Long id);
    Customer saveCustomer(Customer customer);
    void deleteCustomer(Long id);
}
