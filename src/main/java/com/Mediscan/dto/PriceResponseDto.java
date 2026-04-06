package com.Mediscan.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceResponseDto implements Serializable {
    private String platform;
    private BigDecimal price;
    private String productUrl;
    private String productName;
    private Integer stripSize;
    /** true = price is a real scraped value; false = search redirect only */
    @Builder.Default
    private boolean priceAvailable = true;
}
