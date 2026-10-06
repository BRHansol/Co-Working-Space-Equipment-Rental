package com.example.roombooking.controller.web.form;

import java.util.LinkedHashMap;
import java.util.Map;

/** A zero or blank quantity means that the optional item is not selected. */
public class BookingEquipmentForm {
    private Map<Long, Integer> quantities = new LinkedHashMap<>();

    public Map<Long, Integer> getQuantities() { return quantities; }
    public void setQuantities(Map<Long, Integer> quantities) {
        this.quantities = quantities == null ? new LinkedHashMap<>() : quantities;
    }
}
