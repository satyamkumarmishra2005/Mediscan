package com.Mediscan.service;
import com.Mediscan.dto.*;
import com.Mediscan.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
@Service
public class GeminiService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    @Value("${gemini.api.key}")
    private String apiKey;
    @Value("${gemini.api.url}")
    private String apiUrl;
    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    public MedicineDetailsDto parseMedicineFromText(String ocrText) {
        String prompt = """
            You are a pharmaceutical expert. Analyze the following text extracted from a medicine label/packaging 
            and extract the medicine details.
            
            Extract:
            - brandName: The brand/trade name of the medicine
            - genericName: The generic/scientific name
            - saltComposition: The active ingredient(s) and their amounts
            - manufacturer: The manufacturing company
            - dosage: The dosage form and strength (e.g., "500mg tablet")
            
            Text from medicine label:
            \"\"\"
            %s
            \"\"\"
            
            IMPORTANT: Respond ONLY with a valid JSON object. No markdown, no explanation, just pure JSON.
            Example format:
            {"brandName":"Crocin","genericName":"Paracetamol","saltComposition":"Paracetamol 500mg","manufacturer":"GSK","dosage":"500mg Tablet"}
            """.formatted(ocrText);
        try {
            GeminiRequest request = GeminiRequest.builder()
                    .contents(List.of(
                            GeminiRequest.Content.builder()
                                    .parts(List.of(
                                            GeminiRequest.Part.builder().text(prompt).build()
                                    ))
                                    .build()
                    ))
                    .build();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String url = apiUrl + "?key=" + apiKey;
            HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<GeminiResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, GeminiResponse.class
            );
            GeminiResponse geminiResponse = response.getBody();
            if (geminiResponse == null || geminiResponse.getCandidates() == null
                    || geminiResponse.getCandidates().isEmpty()) {
                throw new ApiException("Empty response from Gemini API");
            }
            String jsonText = geminiResponse.getCandidates().get(0)
                    .getContent().getParts().getFirst().getText();
            // Clean up response — Gemini sometimes wraps JSON in markdown code blocks
            jsonText = jsonText.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            return objectMapper.readValue(jsonText, MedicineDetailsDto.class);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Failed to parse medicine details via Gemini", e);
        }
    }
}