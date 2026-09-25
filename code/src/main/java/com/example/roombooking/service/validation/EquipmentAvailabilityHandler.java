package com.example.roombooking.service.validation;

public class EquipmentAvailabilityHandler extends BookingValidationHandler {
    private BookingValidationHandler next;

    public BookingValidationHandler setNext(BookingValidationHandler next) {
        this.next = next;
        return next;
    }

    public final void handleChain(BookingValidationContext context) {
        doValidate(context);
        if (next != null) {
            next.handle(context);
        }
    }

    @Override
    protected void doValidate(BookingValidationContext context) {
        // no-op placeholder for now
    }
}
