package com.example.staj.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.staj.Policy;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
}
