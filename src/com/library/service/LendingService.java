package com.library.service;

import com.library.domain.model.Loan;

import java.util.List;

/**
 * The checkout and return process.
 */
public interface LendingService {

    /**
     * Lends any free copy of an item to a patron.
     *
     * @param patronId the borrower
     * @param itemId   the work wanted
     * @param branchId the branch to prefer, or {@code null} for any
     * @return the new loan
     */
    Loan checkout(String patronId, String itemId, String branchId);

    /**
     * Lends one specific copy.
     *
     * @param patronId the borrower
     * @param barcode  the copy to lend
     * @return the new loan
     */
    Loan checkoutCopy(String patronId, String barcode);

    /**
     * Takes a copy back, charges any fine, and releases the copy to the next
     * reservation in the queue if there is one.
     *
     * @param barcode the copy being returned
     * @return the closed loan
     */
    Loan returnCopy(String barcode);

    /**
     * @param loanId the loan to extend
     * @return the extended loan
     */
    Loan renew(String loanId);

    /**
     * @param patronId the borrower
     * @return the loans that patron currently holds
     */
    List<Loan> getActiveLoans(String patronId);

    /**
     * @return every loan past its due date
     */
    List<Loan> findOverdueLoans();

    /**
     * Publishes an overdue event for each late loan, which is what triggers
     * reminder notifications.
     *
     * @return how many loans were flagged
     */
    int flagOverdueLoans();

    /**
     * @param loanId the loan
     * @return the fine owed as things stand today
     */
    double calculateOutstandingFine(String loanId);
}
