package com.mycart.service.model;

import java.math.BigDecimal;

public class ItemPrice {

    private BigDecimal basePrice;  // Changed to BigDecimal for precision
    private BigDecimal sellingPrice;  // Changed to BigDecimal for precision

    // Getters and Setters

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }
}
