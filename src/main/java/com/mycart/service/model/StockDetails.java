package com.mycart.service.model;

public class StockDetails {

    private int availableStock;
    private String unitOfMeasure;
    private int soldOut;
    private int damaged;

    // Getters and Setters

    public int getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(int availableStock) {
        this.availableStock = availableStock;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public int getSoldOut() {
        return soldOut;
    }

    public void setSoldOut(int soldOut) {
        this.soldOut = soldOut;
    }

    public int getDamaged() {
        return damaged;
    }

    public void setDamaged(int damaged) {
        this.damaged = damaged;
    }
}
