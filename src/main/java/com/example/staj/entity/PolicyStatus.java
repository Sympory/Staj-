package com.example.staj.entity;

public enum PolicyStatus {
    ACTIVE,
    PASSIVE,            // <-- ekle
    CANCELLED,
    // İstersen bunları da kullan:
    DRAFT,              // <-- ekle (opsiyonel)
    PENDING_APPROVAL    // <-- ekle (opsiyonel)
}
