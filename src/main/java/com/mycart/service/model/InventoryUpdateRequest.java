package com.mycart.service.model;

import java.util.List;

public class InventoryUpdateRequest {

    private List<InventoryItem> items;  // Changed to List<InventoryItem>

    // Getters and Setters

    public List<InventoryItem> getItems() {
        return items;
    }

    public void setItems(List<InventoryItem> items) {
        this.items = items;
    }
}
