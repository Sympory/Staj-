package com.example.staj;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    // Özel sorgular istersen buraya ekleyebilirsin
}
