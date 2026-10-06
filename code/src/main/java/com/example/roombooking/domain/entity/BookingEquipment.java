package com.example.roombooking.domain.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking_equipment",
        indexes = {
                @Index(name = "idx_booking_equipment_booking", columnList = "booking_id"),
                @Index(name = "idx_booking_equipment_equipment", columnList = "equipment_id")
        },
        check = @CheckConstraint(name = "ck_booking_equipment_quantity", constraint = "quantity > 0"))
public class BookingEquipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-One ไปยัง Booking (FK: booking_id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_equipment_booking"))
    private Booking booking;

    // Many-to-One ไปยัง Equipment (FK: equipment_id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_equipment_equipment"))
    private Equipment equipment;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public BookingEquipment() {
    }

    public BookingEquipment(Long id, Booking booking, Equipment equipment, Integer quantity) {
        this.id = id;
        this.booking = booking;
        this.equipment = equipment;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    
    
    
}
