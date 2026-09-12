package com.library.policy;

/**
 * Strategy pattern: the lending rules that apply to one membership tier.
 *
 * <p>Keeping these behind an interface means a change of terms — a longer loan
 * for students, a different fine rate — is a new or edited policy class, not a
 * change inside the lending service.</p>
 */
public interface LoanPolicy {

    /**
     * @return how many days a loan runs for
     */
    int getLoanPeriodDays();

    /**
     * @return how many loans a patron may hold at once
     */
    int getMaxConcurrentLoans();

    /**
     * @return the charge per day for a late return
     */
    double getFinePerDay();

    /**
     * @return how many times a loan may be renewed
     */
    int getMaxRenewals();

    /**
     * @return a label for menus and receipts
     */
    String getPolicyName();

    /**
     * @param daysOverdue whole days past the due date
     * @return the fine owed
     */
    default double calculateFine(long daysOverdue) {
        return daysOverdue <= 0 ? 0.0 : daysOverdue * getFinePerDay();
    }
}
