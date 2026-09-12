package com.library.policy;

/**
 * Standard terms for an ordinary member.
 */
public class BasicLoanPolicy implements LoanPolicy {

    @Override
    public int getLoanPeriodDays() {
        return 14;
    }

    @Override
    public int getMaxConcurrentLoans() {
        return 3;
    }

    @Override
    public double getFinePerDay() {
        return 5.0;
    }

    @Override
    public int getMaxRenewals() {
        return 1;
    }

    @Override
    public String getPolicyName() {
        return "Basic";
    }
}
