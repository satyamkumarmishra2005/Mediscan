package com.Mediscan.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "generic_alternatives")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenericAlternative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;  // ← links to the original branded medicine

    @Column(name = "salt_composition", length = 500)
    private String saltComposition;

    @Column(name = "brand_name")
    private String brandName;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private String manufacturer;
}
