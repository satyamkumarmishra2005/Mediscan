package com.Mediscan.service;

import com.Mediscan.dto.OcrResponse;
import com.Mediscan.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OcrService {

    private final RestTemplate restTemplate;

    public OcrService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${ocr.api.key}")
    private String apiKey;

    @Value("${ocr.api.url}")
    private String apiUrl;

    public String extractText(MultipartFile imageFile){
        try{
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("apikey", apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", imageFile.getResource());
            body.add("language", "eng");
            body.add("isOverlayRequired", "false");
            body.add("OCREngine", "2");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<OcrResponse> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    OcrResponse.class
            );

            OcrResponse ocrResponse = response.getBody();
            if (ocrResponse == null || ocrResponse.isErroredOnProcessing()) {
                throw new ApiException("OCR processing failed: " +
                        (ocrResponse != null ? ocrResponse.getErrorMessage() : "No response"));
            }

            if (ocrResponse.getParsedResults() == null || ocrResponse.getParsedResults().isEmpty()) {
                throw new ApiException("No text extracted from image");
            }
            return ocrResponse.getParsedResults().get(0).getParsedText();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("OCR.space API call failed", e);
        }
    }


        }


