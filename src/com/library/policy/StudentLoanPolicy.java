package com.library.policy;

/**
 * Longer loans and a reduced fine for student members.
 */
public class StudentLoanPolicy implements LoanPolicy {

    @Override
    public int getLoanPeriodDays() {
        return 28;
    }

    @Override
    public int getMaxConcurrentLoans() {
        return 5;
    }

    @Override
    public double getFinePerDay() {
        return 2.0;
    }

    @Override
    public int getMaxRenewals() {
        return 2;
    }

    @Override
    public String getPolicyName() {
        return "Student";
    }
}
