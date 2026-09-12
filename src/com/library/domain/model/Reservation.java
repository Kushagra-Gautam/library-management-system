package com.library.domain.model;

import com.library.domain.enums.ReservationStatus;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A patron's request for an item whose copies are all out on loan.
 * Reservations for the same item are served first come, first served.
 */
public class Reservation {

    private final String reservationId;
    private final String itemId;
    private final String patronId;
    private final String branchId;
    private final LocalDate placedOn;
    private ReservationStatus status;
    private LocalDate readyOn;
    private String heldBarcode;

    public Reservation(String reservationId, String itemId, String patronId, String branchId) {
        this.reservationId = reservationId;
        this.itemId = itemId;
        this.patronId = patronId;
        this.branchId = branchId;
        this.placedOn = LocalDate.now();
        this.status = ReservationStatus.WAITING;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getPatronId() {
        return patronId;
    }

    public String getBranchId() {
        return branchId;
    }

    public LocalDate getPlacedOn() {
        return placedOn;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDate getReadyOn() {
        return readyOn;
    }

    public void setReadyOn(LocalDate readyOn) {
        this.readyOn = readyOn;
    }

    /**
     * @return the barcode of the copy being held, once one has been allocated
     */
    public String getHeldBarcode() {
        return heldBarcode;
    }

    public void setHeldBarcode(String heldBarcode) {
        this.heldBarcode = heldBarcode;
    }

    public boolean isWaiting() {
        return status == ReservationStatus.WAITING;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Objects.equals(reservationId, ((Reservation) other).reservationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId);
    }

    @Override
    public String toString() {
        return String.format("%-8s | item %-17s | %-8s | placed %s | %s",
                reservationId, itemId, patronId, placedOn, status.getLabel());
    }
}
