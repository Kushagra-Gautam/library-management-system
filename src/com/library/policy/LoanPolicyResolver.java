package com.library.policy;

import com.library.domain.enums.MembershipType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Maps a membership tier to the policy that governs it.
 *
 * <p>An {@link EnumMap} rather than a switch, so registering a policy for a new
 * tier is a map entry rather than an edit to a conditional. The lending service
 * asks this resolver and never names a concrete policy class.</p>
 */
public class LoanPolicyResolver {

    private final Map<MembershipType, LoanPolicy> policies = new EnumMap<>(MembershipType.class);
    private final LoanPolicy fallback = new BasicLoanPolicy();

    public LoanPolicyResolver() {
        policies.put(MembershipType.BASIC, new BasicLoanPolicy());
        policies.put(MembershipType.STUDENT, new StudentLoanPolicy());
        policies.put(MembershipType.PREMIUM, new PremiumLoanPolicy());
    }

    /**
     * @param type   the tier to govern
     * @param policy the rules to apply to it
     */
    public void register(MembershipType type, LoanPolicy policy) {
        policies.put(type, policy);
    }

    /**
     * @param type the member's tier
     * @return the policy for that tier, falling back to basic terms
     */
    public LoanPolicy resolve(MembershipType type) {
        return policies.getOrDefault(type, fallback);
    }
}
