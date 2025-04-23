package com.mycart.service.model;

public class InventoryItem {

    private String id;  // Changed to `id` for consistency
    private StockDetails stockDetails;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public StockDetails getStockDetails() {
        return stockDetails;
    }

    public void setStockDetails(StockDetails stockDetails) {
        this.stockDetails = stockDetails;
    }
}
