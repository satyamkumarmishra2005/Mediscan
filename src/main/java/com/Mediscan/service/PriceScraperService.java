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

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PriceScraperService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // ──────────────────────────────────────────────
    //  PLATFORM 1: 1mg (Internal Search API)
    // ──────────────────────────────────────────────

    public List<PriceResponseDto> scrapeFromOneMg(String medicineName) {
        List<PriceResponseDto> results = new ArrayList<>();
        try {
            String encodedName = URLEncoder.encode(medicineName, StandardCharsets.UTF_8);
            String apiUrl = "https://www.1mg.com/pwa-dweb-api/api/v4/search/all?q="
                    + encodedName + "&city=New%20Delhi&per_page=5&types=sku,allopathy";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept", "application/json");
            headers.set("Referer", "https://www.1mg.com/");
            headers.set("x-city", "New Delhi");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(apiUrl), HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            // 1mg API returns: { data: { search_results: [ { name, prices: { mrp, ... }, slug } ] } }
            JsonNode searchResults = root.path("data").path("search_results");
            if (searchResults.isMissingNode()) {
                // Fallback: try alternate path
                searchResults = root.path("data").path("skus");
            }

            if (searchResults.isArray()) {
                for (JsonNode item : searchResults) {
                    try {
                        String name = item.path("name").asText("");
                        double price = item.path("prices").path("mrp").asDouble(0);
                        if (price == 0) {
                            price = item.path("prices").path("discounted_price").asDouble(0);
                        }
                        if (price == 0) {
                            price = item.path("price").asDouble(0);
                        }
                        String slug = item.path("slug").asText("");
                        String productUrl = slug.isEmpty() ? "" : "https://www.1mg.com/" + slug;

                        if (!name.isEmpty() && price > 0) {
                            results.add(PriceResponseDto.builder()
                                    .platform("1mg")
                                    .productName(name)
                                    .price(BigDecimal.valueOf(price))
                                    .productUrl(productUrl)
                                    .build());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse 1mg item: {}", e.getMessage());
                    }
                    if (results.size() >= 5) break;
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch 1mg API for '{}': {}", medicineName, e.getMessage());
        }
        return results;
    }

    // ──────────────────────────────────────────────
    //  PLATFORM 2: PharmEasy (TypeAhead Search API)
    // ──────────────────────────────────────────────

    public List<PriceResponseDto> scrapeFromPharmEasy(String medicineName) {
        List<PriceResponseDto> results = new ArrayList<>();
        try {
            String encodedName = URLEncoder.encode(medicineName, StandardCharsets.UTF_8);
            String apiUrl = "https://pharmeasy.in/api/search/searchTypeAhead?q=" + encodedName;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept", "application/json");
            headers.set("Referer", "https://pharmeasy.in/");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(apiUrl), HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            // PharmEasy API returns: { data: { products: [ { name, mrp, slug } ] } }
            JsonNode products = root.path("data").path("products");
            if (products.isMissingNode() || !products.isArray()) {
                products = root.path("data").path("suggestions");
            }

            if (products.isArray()) {
                for (JsonNode item : products) {
                    try {
                        String name = item.path("name").asText("");
                        if (name.isEmpty()) {
                            name = item.path("productName").asText("");
                        }
                        double price = item.path("mrp").asDouble(0);
                        if (price == 0) {
                            price = item.path("price").asDouble(0);
                        }
                        if (price == 0) {
                            price = item.path("salePrice").asDouble(0);
                        }
                        String slug = item.path("slug").asText("");
                        String productUrl = slug.isEmpty() ? ""
                                : "https://pharmeasy.in/online-medicine-order/" + slug;

                        if (!name.isEmpty() && price > 0) {
                            results.add(PriceResponseDto.builder()
                                    .platform("PharmEasy")
                                    .productName(name)
                                    .price(BigDecimal.valueOf(price))
                                    .productUrl(productUrl)
                                    .build());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse PharmEasy item: {}", e.getMessage());
                    }
                    if (results.size() >= 5) break;
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch PharmEasy API for '{}': {}", medicineName, e.getMessage());
        }
        return results;
    }

    // ──────────────────────────────────────────────
    //  PLATFORM 3: Netmeds (Catalog Search API)
    // ──────────────────────────────────────────────

    public List<PriceResponseDto> scrapeFromNetmeds(String medicineName) {
        List<PriceResponseDto> results = new ArrayList<>();
        try {
            String encodedName = URLEncoder.encode(medicineName, StandardCharsets.UTF_8);
            String apiUrl = "https://www.netmeds.com/ext/search/application/api/v1.0/products?q="
                    + encodedName + "&page_size=5";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Referer", "https://www.netmeds.com/");
            headers.set("Origin", "https://www.netmeds.com");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(apiUrl), HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            // Netmeds API returns: { payLoad: { productList: [ { productName, mrp, ... } ] } }
            // OR: { items: [ { attributes: { "mstar-displaynamewops": name, "mstar-sellingprice": price } } ] }
            JsonNode productList = root.path("payLoad").path("productList");
            if (productList.isMissingNode() || !productList.isArray()) {
                productList = root.path("items");
            }

            if (productList.isArray()) {
                for (JsonNode item : productList) {
                    try {
                        String name = item.path("productName").asText("");
                        if (name.isEmpty()) {
                            name = item.path("attributes").path("mstar-displaynamewops").asText("");
                        }
                        if (name.isEmpty()) {
                            name = item.path("name").asText("");
                        }

                        double price = item.path("finalPrice").asDouble(0);
                        if (price == 0) {
                            price = item.path("mrp").asDouble(0);
                        }
                        if (price == 0) {
                            price = item.path("attributes").path("mstar-sellingprice").asDouble(0);
                        }

                        String slug = item.path("productUrl").asText("");
                        if (slug.isEmpty()) {
                            slug = item.path("slug").asText("");
                        }
                        String productUrl = slug.isEmpty() ? ""
                                : (slug.startsWith("http") ? slug : "https://www.netmeds.com" + slug);

                        if (!name.isEmpty() && price > 0) {
                            results.add(PriceResponseDto.builder()
                                    .platform("Netmeds")
                                    .productName(name)
                                    .price(BigDecimal.valueOf(price))
                                    .productUrl(productUrl)
                                    .build());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse Netmeds item: {}", e.getMessage());
                    }
                    if (results.size() >= 5) break;
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch Netmeds API for '{}': {}", medicineName, e.getMessage());
        }
        return results;
    }

    // ──────────────────────────────────────────────
    //  PLATFORM 4: Apollo Pharmacy (Full Search API)
    // ──────────────────────────────────────────────

    public List<PriceResponseDto> scrapeFromApollo(String medicineName) {
        List<PriceResponseDto> results = new ArrayList<>();
        try {
            String apiUrl = "https://search.apollo247.com/v4/fullSearch";

            // Apollo uses a POST request with JSON body
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            headers.set("Referer", "https://www.apollopharmacy.in/");
            headers.set("Origin", "https://www.apollopharmacy.in");

            // Build the search request body
            String requestBody = """
                {
                    "query": "%s",
                    "page": 1,
                    "productsPerPage": 5,
                    "selSortBy": "relevance",
                    "filters": [],
                    "pincode": ""
                }
                """.formatted(medicineName.replace("\"", "\\\""));

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(apiUrl), HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            // Apollo API returns: { data: { productDetails: { products: [ { name, price, specialPrice, urlKey } ] } } }
            JsonNode products = root.path("data").path("productDetails").path("products");
            if (products.isMissingNode() || !products.isArray()) {
                products = root.path("data").path("products");
            }

            if (products.isArray()) {
                for (JsonNode item : products) {
                    try {
                        String name = item.path("name").asText("");
                        double price = item.path("specialPrice").asDouble(0);
                        if (price == 0) {
                            price = item.path("price").asDouble(0);
                        }
                        String urlKey = item.path("urlKey").asText("");
                        String productUrl = urlKey.isEmpty() ? ""
                                : "https://www.apollopharmacy.in/otc/" + urlKey;

                        if (!name.isEmpty() && price > 0) {
                            results.add(PriceResponseDto.builder()
                                    .platform("Apollo Pharmacy")
                                    .productName(name)
                                    .price(BigDecimal.valueOf(price))
                                    .productUrl(productUrl)
                                    .build());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse Apollo item: {}", e.getMessage());
                    }
                    if (results.size() >= 5) break;
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch Apollo API for '{}': {}", medicineName, e.getMessage());
        }
        return results;
    }

    // ──────────────────────────────────────────────
    //  AGGREGATE: All 4 platforms (with polite delays)
    // ──────────────────────────────────────────────

    public List<PriceResponseDto> scrapeAllPlatforms(String medicineName) {
        List<PriceResponseDto> allResults = new ArrayList<>();

        allResults.addAll(scrapeFromOneMg(medicineName));
        politeSleep();

        allResults.addAll(scrapeFromPharmEasy(medicineName));
        politeSleep();

        allResults.addAll(scrapeFromNetmeds(medicineName));
        politeSleep();

        allResults.addAll(scrapeFromApollo(medicineName));

        return allResults;
    }

    private void politeSleep() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}



