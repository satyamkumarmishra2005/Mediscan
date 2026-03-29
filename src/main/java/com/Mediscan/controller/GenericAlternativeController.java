package com.Mediscan.controller;


import com.Mediscan.dto.GenericAlternativeDto;
import com.Mediscan.model.Medicine;
import com.Mediscan.repository.MedicineRepository;
import com.Mediscan.service.GenericAlternativeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/generics")
@CrossOrigin("*")
public class GenericAlternativeController {

    private final GenericAlternativeService genericAlternativeService;
    private final MedicineRepository medicineRepository;
    public GenericAlternativeController(GenericAlternativeService genericAlternativeService,
                                        MedicineRepository medicineRepository) {
        this.genericAlternativeService = genericAlternativeService;
        this.medicineRepository = medicineRepository;
    }

    /**
     * Find generic alternatives by medicine ID.
     * This is the primary endpoint — used after /identify returns a medicine.
     *
     * Example: GET /api/v1/generics/25dcaa32-50fd-45e0-b1b2-c098f29d5731
     */

    @GetMapping("/{medicineId}")
    public ResponseEntity<List<GenericAlternativeDto>> getAlternativesById(
            @PathVariable UUID medicineId){
        List<GenericAlternativeDto> alternatives = genericAlternativeService.findAlternative(medicineId);
        return ResponseEntity.ok(alternatives);

    }


    /**
     * Find generic alternatives by salt composition name.
     * Fallback endpoint — useful for direct lookup without a medicine ID.
     *
     * Example: GET /api/v1/generics?salt=Paracetamol
     */

    @GetMapping
    public ResponseEntity<List<GenericAlternativeDto>> getAlternativesBySalt(
            @RequestParam String salt) {
        // Find all medicines with this salt composition
        List<Medicine> medicines = medicineRepository
                .findBySaltCompositionContainingIgnoreCase(salt);
        // Map to DTOs
        List<GenericAlternativeDto> alternatives = medicines.stream()
                .map(m -> GenericAlternativeDto.builder()
                        .brandName(m.getBrandName())
                        .genericName(m.getGenericName())
                        .saltComposition(m.getSaltComposition())
                        .manufacturer(m.getManufacturer())
                        .dosage(m.getDosage())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(alternatives);
    }





}
