package com.library.domain.model;

import com.library.domain.enums.CopyStatus;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A single physical copy of a catalogued item, identified by its barcode.
 *
 * <p>Separating the copy from the {@link LibraryItem} is the decision that makes
 * the rest of the system straightforward: a library owns one catalogue record
 * for "Clean Code" but four copies of it, possibly in different branches, each
 * with its own status. Availability, lending and transfers are all properties of
 * a copy, never of the catalogue record.</p>
 */
public class ItemCopy {

    private final String barcode;
    private final String itemId;
    private String branchId;
    private CopyStatus status;
    private final LocalDate acquiredOn;

    public ItemCopy(String barcode, String itemId, String branchId) {
        this.barcode = barcode;
        this.itemId = itemId;
        this.branchId = branchId;
        this.status = CopyStatus.AVAILABLE;
        this.acquiredOn = LocalDate.now();
    }

    public String getBarcode() {
        return barcode;
    }

    /**
     * @return the catalogue key of the work this is a copy of
     */
    public String getItemId() {
        return itemId;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public CopyStatus getStatus() {
        return status;
    }

    public void setStatus(CopyStatus status) {
        this.status = status;
    }

    public LocalDate getAcquiredOn() {
        return acquiredOn;
    }

    public boolean isAvailable() {
        return status.isLendable();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Objects.equals(barcode, ((ItemCopy) other).barcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barcode);
    }

    @Override
    public String toString() {
        return String.format("%-10s | item %-17s | %-8s | %s",
                barcode, itemId, branchId, status.getLabel());
    }
}
