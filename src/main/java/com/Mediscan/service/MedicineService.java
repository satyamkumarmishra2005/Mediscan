package com.Mediscan.service;

import com.Mediscan.dto.MedicineDetailsDto;
import com.Mediscan.exception.MedicineNotFoundException;
import com.Mediscan.model.Medicine;
import com.Mediscan.repository.MedicineRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class MedicineService {

    private final OcrService ocrService;
    private final GeminiService geminiService;
    private final MedicineRepository medicineRepository;
    public MedicineService(OcrService ocrService, GeminiService geminiService,
                           MedicineRepository medicineRepository) {
        this.ocrService = ocrService;
        this.geminiService = geminiService;
        this.medicineRepository = medicineRepository;
    }


    public Medicine identifyMedicine(MultipartFile imageFile){
        // Step 1: Extract text from image via OCR.space
        String ocrText = ocrService.extractText(imageFile);

        // Step 2: Parse medicine details from text via Gemini
        MedicineDetailsDto details = geminiService.parseMedicineFromText(ocrText);

        // Step 3: Check if this medicine already exists in DB
        List<Medicine> existing = medicineRepository.findByBrandNameContainingIgnoreCase(details.getBrandName());

        if(!existing.isEmpty()){
            return existing.get(0);  // Return existing record
        }


        // Step 4: Save new medicine to database
        Medicine medicine = Medicine.builder()
                .brandName(details.getBrandName())
                .genericName(details.getGenericName())
                .saltComposition(details.getSaltComposition())
                .manufacturer(details.getManufacturer())
                .dosage(details.getDosage())
                .build();
        return medicineRepository.save(medicine);
    }

    public Medicine getMedicineById(UUID id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new MedicineNotFoundException("Medicine not found with id: " + id));
    }

    public List<Medicine> searchMedicines(String query) {
        return medicineRepository.findByBrandNameContainingIgnoreCase(query);
    }
}
