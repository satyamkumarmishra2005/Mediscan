package com.Mediscan.service;


import com.Mediscan.dto.PriceResponseDto;
import com.Mediscan.model.Medicine;
import com.Mediscan.model.MedicinePrice;
import com.Mediscan.repository.MedicinePriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public List<PriceResponseDto> getPricesForMedicine(UUID medicineId){
        // Step 1: Get the medicine from DB (throws 404 if not found)

        Medicine medicine = medicineService.getMedicineById(medicineId);

        log.info("Cache MISS for medicine '{}' — scraping prices...", medicine.getBrandName());

        // Step2: Scrape prices from all platforms

        List<PriceResponseDto> scrapedPrices = priceScraperService.scrapeAllPlatforms(medicine.getBrandName());

        // Step 3: Save scraped prices to DB (optional, for caching)
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
}
