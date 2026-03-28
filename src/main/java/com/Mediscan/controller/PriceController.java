package com.Mediscan.controller;

import com.Mediscan.dto.PriceResponseDto;
import com.Mediscan.service.PriceService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prices")
@CrossOrigin(origins = "*")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping("/{medicineId}")
    public ResponseEntity<List<PriceResponseDto>> getPrices(@PathVariable UUID medicineId) {
        List<PriceResponseDto> prices = priceService.getPricesForMedicine(medicineId);
        return ResponseEntity.ok(prices);
    }

    // Evict cached prices for a specific medicine (forces re-scrape on next GET)
    @DeleteMapping("/cache/{medicineId}")
    @CacheEvict(value = "medicinePrices", key = "#medicineId.toString()")
    public ResponseEntity<Map<String, String>> evictCache(@PathVariable UUID medicineId) {
        return ResponseEntity.ok(Map.of("message", "Cache cleared for medicine " + medicineId));
    }

    // Evict ALL cached prices (nuclear option)
    @DeleteMapping("/cache/all")
    @CacheEvict(value = "medicinePrices", allEntries = true)
    public ResponseEntity<Map<String, String>> evictAllCache() {
        return ResponseEntity.ok(Map.of("message", "All price caches cleared"));
    }
}
