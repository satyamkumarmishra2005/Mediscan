package com.Mediscan.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medicine")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "generic_name")
    private String genericName;

    @Column(name = "salt_composition", length = 500)
    private String saltComposition;

    private String manufacturer;

    private String dosage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
