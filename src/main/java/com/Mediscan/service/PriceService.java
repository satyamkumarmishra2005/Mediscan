package com.Mediscan.service;


import com.Mediscan.dto.PriceResponseDto;
import com.Mediscan.model.Medicine;
import com.Mediscan.model.MedicinePrice;
import com.Mediscan.repository.MedicinePriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PriceService {

    private static final Logger log = LoggerFactory.getLogger(PriceService.class);

    private final PriceScraperService priceScraperService;
    private final MedicineService medicineService;
    private final MedicinePriceRepository medicinePriceRepository;

    public PriceService(PriceScraperService priceScraperService,
                        MedicineService medicineService,
                        MedicinePriceRepository medicinePriceRepository) {
        this.priceScraperService = priceScraperService;
        this.medicineService = medicineService;
        this.medicinePriceRepository = medicinePriceRepository;
    }

    @Cacheable(value = "medicinePrices", key = "#medicineId.toString()", unless = "#result.isEmpty()")
    public List<PriceResponseDto> getPricesForMedicine(UUID medicineId) {
        Medicine medicine = medicineService.getMedicineById(medicineId);

        log.info("Cache MISS for medicine '{}' — scraping prices...", medicine.getBrandName());

        List<PriceResponseDto> scrapedPrices = priceScraperService.scrapeAllPlatforms(medicine.getBrandName());

        // Delete old stale prices for this medicine before saving fresh ones
        medicinePriceRepository.deleteByMedicineId(medicineId);

        for (PriceResponseDto dto : scrapedPrices) {
            MedicinePrice priceEntity = MedicinePrice.builder()
                    .medicine(medicine)
                    .platform(dto.getPlatform())
                    .price(dto.getPrice())
                    .url(dto.getProductUrl())
                    .stripSize(dto.getStripSize())
                    .build();
            medicinePriceRepository.save(priceEntity);
        }

        log.info("Scraped and saved {} prices for '{}'", scrapedPrices.size(), medicine.getBrandName());
        return scrapedPrices;
    }

    /**
     * Force-evicts the Redis cache for a specific medicine so next call re-scrapes fresh data.
     */
    @CacheEvict(value = "medicinePrices", key = "#medicineId.toString()")
    public void evictCacheForMedicine(UUID medicineId) {
        log.info("Cache evicted for medicine ID: {}", medicineId);
    }

    /**
     * Evicts ALL cached prices — useful after scraper fixes are deployed.
     */
    @CacheEvict(value = "medicinePrices", allEntries = true)
    public void evictAllPriceCache() {
        log.info("All medicinePrices cache entries evicted.");
    }
}
