package com.Mediscan.controller;


import com.Mediscan.model.Medicine;
import com.Mediscan.service.MedicineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medicine")
@CrossOrigin(origins = "*") // Allow frontend to call this API
public class MedicineController {

    private final MedicineService medicineService;
    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @PostMapping("/identify")
    public ResponseEntity<Medicine> identifyMedicine(@RequestParam("image") MultipartFile imageFile) {
        Medicine medicine = medicineService.identifyMedicine(imageFile);
        return ResponseEntity.ok(medicine);
    }

    @PostMapping("/identify/manual")
    public ResponseEntity<Medicine> identifyMedicineManual(@RequestParam("name") String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Medicine medicine = medicineService.identifyMedicineManual(name.trim());
        return ResponseEntity.ok(medicine);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Medicine> getMedicineById(@PathVariable UUID id) {
        Medicine medicine = medicineService.getMedicineById(id);
        return ResponseEntity.ok(medicine);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Medicine>> searchMedicines(@RequestParam String q) {
        List<Medicine> medicines = medicineService.searchMedicines(q);
        return ResponseEntity.ok(medicines);
    }
}
