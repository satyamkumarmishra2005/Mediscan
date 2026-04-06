package com.Mediscan.service;

import com.Mediscan.dto.PriceResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class PriceScraperService {

    private static final Logger log = LoggerFactory.getLogger(PriceScraperService.class);

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PriceScraperService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // ──────────────────────────────────────────────
    //  PLATFORM 1 : Tata 1mg  (real prices via API)
    // ──────────────────────────────────────────────

    public List<PriceResponseDto> scrapeFromOneMg(String medicineName) {
        List<PriceResponseDto> results = new ArrayList<>();
        try {
            String enc = URLEncoder.encode(medicineName, StandardCharsets.UTF_8);
            String url = "https://www.1mg.com/pwa-dweb-api/api/v4/search/all?q="
                    + enc + "&city=New%20Delhi&per_page=5&types=sku,allopathy";

            HttpHeaders h = new HttpHeaders();
            h.set("User-Agent", USER_AGENT);
            h.set("Accept", "application/json");
            h.set("Referer", "https://www.1mg.com/");
            h.set("x-city", "New Delhi");

            JsonNode root = objectMapper.readTree(
                    restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>(h), String.class)
                                .getBody());

            JsonNode items = root.path("data").path("search_results");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    // Skip unavailable / discontinued items
                    if (!item.path("available").asBoolean(true)) continue;

                    String name    = item.path("name").asText("");
                    // Prices come as strings "₹18.9" — strip currency symbol
                    String discStr = item.path("prices").path("discounted_price").asText("");
                    String mrpStr  = item.path("prices").path("mrp").asText("");
                    double price   = parsePrice(discStr.isEmpty() ? mrpStr : discStr);

                    // "url" e.g. "/drugs/crocin-advance-500mg-tablet-600468"
                    String urlPath = item.path("url").asText("");
                    String productUrl = urlPath.isEmpty()
                            ? "https://www.1mg.com/search/all?name=" + enc
                            : "https://www.1mg.com" + urlPath;

                    if (!name.isEmpty() && price > 0) {
                        results.add(PriceResponseDto.builder()
                                .platform("1mg")
                                .productName(name)
                                .price(BigDecimal.valueOf(price))
                                .productUrl(productUrl)
                                .priceAvailable(true)
                                .build());
                    }
                    if (results.size() >= 5) break;
                }
            }
        } catch (Exception e) {
            log.error("1mg scrape failed for '{}': {}", medicineName, e.getMessage());
        }
        log.info("1mg → {} results for '{}'", results.size(), medicineName);
        return results;
    }

    // ──────────────────────────────────────────────
    //  PLATFORM 2 : PharmEasy  (slug from typeahead; no prices in API)
    // ──────────────────────────────────────────────

    public List<PriceResponseDto> scrapeFromPharmEasy(String medicineName) {
        List<PriceResponseDto> results = new ArrayList<>();
        String enc = URLEncoder.encode(medicineName, StandardCharsets.UTF_8);

        try {
            String url = "https://pharmeasy.in/api/search/searchTypeAhead?q=" + enc;

            HttpHeaders h = new HttpHeaders();
            h.set("User-Agent", USER_AGENT);
            h.set("Accept", "application/json");
            h.set("Referer", "https://pharmeasy.in/");

            JsonNode root = objectMapper.readTree(
                    restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>(h), String.class)
                                .getBody());

            JsonNode products = root.path("data").path("products");
            if (products.isArray()) {
                for (JsonNode item : products) {
                    // entityType == 2 → real product (not generic search term)
                    if (item.path("entityType").asInt(0) != 2) continue;

                    String name = item.path("name").asText("");
                    if (name.isEmpty()) continue;

                    String slug = item.path("slug").asText("");
                    if (slug.isEmpty() || slug.contains("%20")) continue;

                    // PharmEasy typeahead gives no price; mark as redirect-only
                    String productUrl = "https://pharmeasy.in/online-medicine-order/" + slug;

                    results.add(PriceResponseDto.builder()
                            .platform("PharmEasy")
                            .productName(name)
                            .price(null)
                            .productUrl(productUrl)
                            .priceAvailable(false)
                            .build());

                    if (results.size() >= 3) break;
                }
            }
        } catch (Exception e) {
            log.error("PharmEasy scrape failed for '{}': {}", medicineName, e.getMessage());
        }

        // Always guarantee at least 1 PharmEasy entry (search-page fallback)
        if (results.isEmpty()) {
            results.add(PriceResponseDto.builder()
                    .platform("PharmEasy")
                    .productName(medicineName)
                    .price(null)
                    .productUrl("https://pharmeasy.in/search/result?q=" + enc)
                    .priceAvailable(false)
                    .build());
        }

        log.info("PharmEasy → {} results for '{}'", results.size(), medicineName);
        return results;
    }

    // ──────────────────────────────────────────────
    //  PLATFORM 3 : Apollo Pharmacy  (search-redirect)
    //  Working URL: /search-medicines/{name}
    // ──────────────────────────────────────────────

    public List<PriceResponseDto> buildApolloEntry(String medicineName) {
        // Apollo uses a clean URL slug format: /search-medicines/crocin+advance
        String enc = URLEncoder.encode(medicineName, StandardCharsets.UTF_8)
                               .replace("%20", "+");   // spaces as + not %20
        return List.of(PriceResponseDto.builder()
                .platform("Apollo Pharmacy")
                .productName(medicineName)
                .price(null)
                .productUrl("https://www.apollopharmacy.in/search-medicines/" + enc)
                .priceAvailable(false)
                .build());
    }

    // ──────────────────────────────────────────────
    //  PLATFORM 4 : MedPlus  (search-redirect)
    //  Working URL: /medicines/search/{name}
    // ──────────────────────────────────────────────

    public List<PriceResponseDto> buildMedPlusEntry(String medicineName) {
        // MedPlus uses Base64 encoded 'A::' + searchterm for its newly updated search URL structure
        String rawQuery = "A::" + medicineName.toLowerCase();
        String b64slug = java.util.Base64.getEncoder().encodeToString(rawQuery.getBytes(StandardCharsets.UTF_8));
        
        return List.of(PriceResponseDto.builder()
                .platform("MedPlus")
                .productName(medicineName)
                .price(null)
                .productUrl("https://www.medplusmart.com/searchAll/" + b64slug)
                .priceAvailable(false)
                .build());
    }

    // ──────────────────────────────────────────────
    //  AGGREGATE
    // ──────────────────────────────────────────────

    public List<PriceResponseDto> scrapeAllPlatforms(String medicineName) {
        List<PriceResponseDto> all = new ArrayList<>();

        all.addAll(scrapeFromOneMg(medicineName));
        politeSleep();

        all.addAll(scrapeFromPharmEasy(medicineName));
        politeSleep();

        // Apollo and MedPlus: always present (search redirect)
        all.addAll(buildApolloEntry(medicineName));
        all.addAll(buildMedPlusEntry(medicineName));

        log.info("Total {} platform entries for '{}'", all.size(), medicineName);
        return all;
    }

    private void politeSleep() {
        try { Thread.sleep(600); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** Strip currency symbols like ₹, Rs., etc. and parse as double. */
    private double parsePrice(String raw) {
        if (raw == null || raw.isBlank()) return 0.0;
        try {
            String cleaned = raw.replaceAll("[^0-9.]", "");
            int dot = cleaned.indexOf('.');
            if (dot >= 0)
                cleaned = cleaned.substring(0, dot + 1) + cleaned.substring(dot + 1).replace(".", "");
            return cleaned.isEmpty() ? 0.0 : Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
