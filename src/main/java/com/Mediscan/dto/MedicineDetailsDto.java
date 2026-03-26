package com.Mediscan.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineDetailsDto {
    private String brandName;
    private String genericName;
    private String saltComposition;
    private String manufacturer;
    private String dosage;
}
