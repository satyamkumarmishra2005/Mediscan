package com.Mediscan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenericAlternativeDto implements Serializable {

    private String brandName;
    private String genericName;
    private String saltComposition;
    private String manufacturer;
    private String dosage;
    private BigDecimal estimatedPrice;

    // How much cheaper than the original branded medicine (e.g., 40.0 = 40% cheaper)
    private Double savingsPercent;
}
