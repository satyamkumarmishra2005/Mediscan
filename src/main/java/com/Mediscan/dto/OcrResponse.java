package com.Mediscan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OcrResponse {

    @JsonProperty("ParsedResults")
    private List<ParsedResult> parsedResults;

    @JsonProperty("IsErroredOnProcessing")
    private boolean erroredOnProcessing;


    @JsonProperty("ErrorMessage")
    private List<String> errorMessage;

    @Data
    public static class ParsedResult {
        @JsonProperty("ParsedText")
        private String parsedText;

        @JsonProperty("ErrorMessage")
        private String errorMessage;

        @JsonProperty("FileParseExitCode")
        private int fileParseExitCode;
    }

}

// @JsonProperty?
//
//OCR.space returns PascalCase keys (ParsedResults, ParsedText) but Java uses camelCase. @JsonProperty maps between them.
