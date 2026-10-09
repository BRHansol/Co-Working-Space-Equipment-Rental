package com.example.roombooking.controller.web.form;

import java.util.LinkedHashMap;
import java.util.Map;

public class BookingEditForm extends BookingDetailsForm {
    private Map<Long, Integer> quantities = new LinkedHashMap<>();

    public Map<Long, Integer> getQuantities() { return quantities; }
    public void setQuantities(Map<Long, Integer> quantities) {
        this.quantities = quantities == null ? new LinkedHashMap<>() : quantities;
    }
}
