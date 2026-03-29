package com.Mediscan.service;


import com.Mediscan.dto.GenericAlternativeDto;
import com.Mediscan.model.Medicine;
import com.Mediscan.repository.MedicineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GenericAlternativeService {

    private static final Logger log = LoggerFactory.getLogger(GenericAlternativeService.class);

    private final MedicineRepository medicineRepository;
    private final GeminiService geminiService;
    private final PriceScraperService priceScraperService;
    public GenericAlternativeService(MedicineRepository medicineRepository,
                                     GeminiService geminiService,
                                     PriceScraperService priceScraperService) {
        this.medicineRepository = medicineRepository;
        this.geminiService = geminiService;
        this.priceScraperService = priceScraperService;
    }

    /**
     * Finds generic alternatives for a medicine by its ID.
     * Uses a two-strategy approach:
     *   1. Search the local DB for medicines with matching salt composition
     *   2. If not enough results, ask Gemini AI for alternatives and save them
     */


    @Cacheable(value = "genericAlternatives", key = "#medicineId.toString()", unless = "#result.isEmpty()")
    public List<GenericAlternativeDto> findAlternative(UUID medicineId){

        // Step 1: Get the original medicine from DB
        Medicine original = medicineRepository.findById(medicineId)
                .orElseThrow(()-> new RuntimeException("Medicine not found:" + medicineId));

        String saltComposition = original.getSaltComposition();

        if(saltComposition== null || saltComposition.isBlank()){
            log.warn("Medicine '{}' has no salt composition — cannot find alternatives", original.getBrandName());
            return List.of();
        }

        log.info("Finding alternatives for '{}' (salt: '{}')", original.getBrandName(), saltComposition);

        // Step 2: Estimate the original medicine's price (from scraped data)

        BigDecimal originalPrice = estimateOriginalPrice(original);

        // Step 3: Strategy 1 — Search DB for medicines with same/similar salt


        List<GenericAlternativeDto> alternatives = findFromDatabase(original, saltComposition);

        log.info("Found {} alternatives in DB for '{}'", alternatives.size(), original.getBrandName());

        // Step 4: Strategy 2 — If DB has fewer than 3 results, ask Gemini

        if(alternatives.size() <3){
            log.info("DB has < 3 alternatives — enriching via Gemini AI...");
        }

        List<GenericAlternativeDto> geminiResults = geminiService.findGenericAlternatives(saltComposition,original.getBrandName());

        // Save Gemini discoveries to DB for future use (self-enriching)

        for (GenericAlternativeDto dto : geminiResults) {
            saveAlternativeToDatabase(dto);
        }

        // Merge: DB results + Gemini results, deduplicated
        alternatives = mergeAndDeduplicate(alternatives, geminiResults);
        log.info("After Gemini enrichment: {} total alternatives", alternatives.size());


    // Step 5: Calculate savings percentage for each alternative
        if (originalPrice != null && originalPrice.compareTo(BigDecimal.ZERO) > 0) {
        for (GenericAlternativeDto alt : alternatives) {
            if (alt.getEstimatedPrice() != null) {
                double savings = originalPrice.subtract(alt.getEstimatedPrice())
                        .divide(originalPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                alt.setSavingsPercent(Math.round(savings * 10.0) / 10.0);  // Round to 1 decimal
            }
        }
    }
        // Step 6: Sort by price (cheapest first)
        alternatives.sort(Comparator.comparing(
                GenericAlternativeDto::getEstimatedPrice,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
        return alternatives;

    }

    /**
     * Strategy 1: Find alternatives from the local database.
     * Searches by extracting the primary salt name from the composition.
     */
    private List<GenericAlternativeDto> findFromDatabase(Medicine original, String saltComposition) {
        // Extract the primary salt name (e.g., "Paracetamol" from "Paracetamol 500mg")
        String primarySalt = extractPrimarySalt(saltComposition);
        List<Medicine> matches = medicineRepository.findBySaltCompositionContainingIgnoreCase(primarySalt);
        return matches.stream()
                .filter(m -> !m.getId().equals(original.getId()))  // Exclude the original
                .map(m -> GenericAlternativeDto.builder()
                        .brandName(m.getBrandName())
                        .genericName(m.getGenericName())
                        .saltComposition(m.getSaltComposition())
                        .manufacturer(m.getManufacturer())
                        .dosage(m.getDosage())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Extracts the primary salt name from a composition string.
     * Examples:
     *   "Paracetamol 500mg" → "Paracetamol"
     *   "Paracetamol 500mg + Caffeine 65mg" → "Paracetamol"
     *   "Amoxicillin Trihydrate 500mg" → "Amoxicillin"
     */
    private String extractPrimarySalt(String saltComposition) {
        // Split by common delimiters: +, comma, &
        String firstSalt = saltComposition.split("[+,&]")[0].trim();
        // Remove numeric parts and units (mg, ml, mcg, etc.)
        String saltName = firstSalt.replaceAll("\\d+(\\.\\d+)?\\s*(mg|ml|mcg|g|iu|%)", "").trim();
        // Take first word if it's a multi-word name like "Amoxicillin Trihydrate"
        // Keep the full name for better matching
        return saltName.isEmpty() ? saltComposition : saltName;
    }

    /**
     * Estimates the original medicine's price by checking recently scraped data.
     * Returns average price across platforms, or null if no data.
     */
    private BigDecimal estimateOriginalPrice(Medicine original) {
        try {
            var prices = priceScraperService.scrapeFromOneMg(original.getBrandName());
            if (!prices.isEmpty()) {
                return prices.get(0).getPrice();
            }
        } catch (Exception e) {
            log.warn("Could not estimate price for '{}': {}", original.getBrandName(), e.getMessage());
        }
        return null;
    }

    /**
     * Saves a Gemini-discovered alternative to the Medicine table.
     * This is the "self-enriching" part — every query adds data.
     */
    private void saveAlternativeToDatabase(GenericAlternativeDto dto) {
        try {
            // Check if already exists (avoid duplicates)
            List<Medicine> existing = medicineRepository.findByBrandNameContainingIgnoreCase(dto.getBrandName());
            if (!existing.isEmpty()) {
                return;  // Already in DB
            }
            Medicine medicine = Medicine.builder()
                    .brandName(dto.getBrandName())
                    .genericName(dto.getGenericName())
                    .saltComposition(dto.getSaltComposition())
                    .manufacturer(dto.getManufacturer())
                    .dosage(dto.getDosage())
                    .build();
            medicineRepository.save(medicine);
            log.info("Saved new alternative to DB: '{}'", dto.getBrandName());
        } catch (Exception e) {
            log.warn("Failed to save alternative '{}': {}", dto.getBrandName(), e.getMessage());
        }
    }

    /**
     * Merges DB results and Gemini results, removing duplicates by brand name.
     */
    private List<GenericAlternativeDto> mergeAndDeduplicate(
            List<GenericAlternativeDto> dbResults,
            List<GenericAlternativeDto> geminiResults) {
        Map<String, GenericAlternativeDto> merged = new LinkedHashMap<>();
        // DB results take priority (they're verified)
        for (GenericAlternativeDto dto : dbResults) {
            merged.put(dto.getBrandName().toLowerCase(), dto);
        }
        // Gemini results fill gaps
        for (GenericAlternativeDto dto : geminiResults) {
            merged.putIfAbsent(dto.getBrandName().toLowerCase(), dto);
        }
        return new ArrayList<>(merged.values());
    }

}
