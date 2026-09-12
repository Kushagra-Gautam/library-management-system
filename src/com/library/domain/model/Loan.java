package com.library.domain.model;

import com.library.domain.enums.LoanStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A record of one copy being lent to one patron.
 *
 * <p>The loan knows how to answer questions about itself — whether it is
 * overdue, by how many days — but it does not know the fine rate. That belongs
 * to the loan policy, so the rate can change without touching this class.</p>
 */
public class Loan {

    private final String loanId;
    private final String barcode;
    private final String itemId;
    private final String patronId;
    private final String branchId;
    private final LocalDate borrowedOn;
    private LocalDate dueOn;
    private LocalDate returnedOn;
    private LoanStatus status;
    private double fineCharged;

    public Loan(String loanId, String barcode, String itemId, String patronId,
                String branchId, LocalDate borrowedOn, LocalDate dueOn) {
        this.loanId = loanId;
        this.barcode = barcode;
        this.itemId = itemId;
        this.patronId = patronId;
        this.branchId = branchId;
        this.borrowedOn = borrowedOn;
        this.dueOn = dueOn;
        this.status = LoanStatus.ACTIVE;
    }

    public String getLoanId() {
        return loanId;
    }

    public String getBarcode() {
        return barcode;
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

    public LocalDate getBorrowedOn() {
        return borrowedOn;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public void setDueOn(LocalDate dueOn) {
        this.dueOn = dueOn;
    }

    public LocalDate getReturnedOn() {
        return returnedOn;
    }

    public void setReturnedOn(LocalDate returnedOn) {
        this.returnedOn = returnedOn;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public double getFineCharged() {
        return fineCharged;
    }

    public void setFineCharged(double fineCharged) {
        this.fineCharged = fineCharged;
    }

    public boolean isActive() {
        return status == LoanStatus.ACTIVE || status == LoanStatus.OVERDUE;
    }

    /**
     * @param asOf the date to judge against
     * @return whole days past the due date, or zero when not overdue
     */
    public long daysOverdue(LocalDate asOf) {
        LocalDate reference = returnedOn != null ? returnedOn : asOf;
        if (!reference.isAfter(dueOn)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueOn, reference);
    }

    public boolean isOverdue(LocalDate asOf) {
        return isActive() && daysOverdue(asOf) > 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Objects.equals(loanId, ((Loan) other).loanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId);
    }

    @Override
    public String toString() {
        return String.format("%-8s | copy %-10s | %-8s | out %s | due %s | %s",
                loanId, barcode, patronId, borrowedOn, dueOn, status.getLabel());
    }
}
