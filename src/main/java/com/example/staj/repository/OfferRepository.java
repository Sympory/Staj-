package com.example.staj.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.staj.Offer;

public interface OfferRepository extends JpaRepository<Offer, Long> {
}
