package com.Mediscan.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medicine_prices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicinePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    private String platform;  // '1mg', 'PharmEasy', 'Apollo'

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String url;

    @Column(name = "strip_size")
    private Integer stripSize;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    protected void onCreate() {
        lastUpdated = LocalDateTime.now();
    }
}
