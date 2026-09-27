package com.example.roombooking.dto.request;

public class EquipmentCreateRequest {
    private String name;
    private Integer totalQuantity;
    private String category;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}