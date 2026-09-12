package com.library.repository;

import com.library.domain.model.Loan;

import java.util.List;
import java.util.Optional;

/**
 * Storage for loans.
 */
public interface LoanRepository extends Repository<Loan, String> {

    /**
     * @param patronId the borrower
     * @return every loan that patron has ever held
     */
    List<Loan> findByPatron(String patronId);

    /**
     * @param patronId the borrower
     * @return only the loans that patron currently holds
     */
    List<Loan> findActiveByPatron(String patronId);

    /**
     * @param barcode the copy
     * @return the open loan against that copy, if there is one
     */
    Optional<Loan> findActiveByBarcode(String barcode);

    /**
     * @return every loan not yet returned
     */
    List<Loan> findAllActive();
}
