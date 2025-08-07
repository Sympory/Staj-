package com.example.staj;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String policyNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Getter ve Setter metotları
}
