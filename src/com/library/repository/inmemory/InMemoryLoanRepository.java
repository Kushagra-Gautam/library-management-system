package com.library.repository.inmemory;

import com.library.domain.model.Loan;
import com.library.repository.LoanRepository;

import java.util.List;
import java.util.Optional;

/**
 * In-memory loan ledger.
 */
public class InMemoryLoanRepository extends InMemoryRepository<Loan, String>
        implements LoanRepository {

    public InMemoryLoanRepository() {
        super(Loan::getLoanId);
    }

    @Override
    public List<Loan> findByPatron(String patronId) {
        return filter(loan -> loan.getPatronId().equals(patronId));
    }

    @Override
    public List<Loan> findActiveByPatron(String patronId) {
        return filter(loan -> loan.getPatronId().equals(patronId) && loan.isActive());
    }

    @Override
    public Optional<Loan> findActiveByBarcode(String barcode) {
        return firstMatching(loan -> loan.getBarcode().equals(barcode) && loan.isActive());
    }

    @Override
    public List<Loan> findAllActive() {
        return filter(Loan::isActive);
    }
}
