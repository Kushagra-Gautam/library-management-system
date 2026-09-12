package com.library.policy;

/**
 * The most generous terms, with a grace period before fines begin.
 */
public class PremiumLoanPolicy implements LoanPolicy {

    private static final int GRACE_DAYS = 3;

    @Override
    public int getLoanPeriodDays() {
        return 21;
    }

    @Override
    public int getMaxConcurrentLoans() {
        return 10;
    }

    @Override
    public double getFinePerDay() {
        return 3.0;
    }

    @Override
    public int getMaxRenewals() {
        return 3;
    }

    @Override
    public String getPolicyName() {
        return "Premium";
    }

    /**
     * Overrides the default so the first few late days are forgiven.
     */
    @Override
    public double calculateFine(long daysOverdue) {
        long chargeable = daysOverdue - GRACE_DAYS;
        return chargeable <= 0 ? 0.0 : chargeable * getFinePerDay();
    }
}
