package com.library.domain.enums;

/**
 * Membership tier. The tier selects the loan policy rather than carrying the
 * lending rules itself, which keeps policy changes out of the enum.
 */
public enum MembershipType {

    BASIC("Basic"),
    STUDENT("Student"),
    PREMIUM("Premium");

    private final String label;

    MembershipType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static MembershipType fromText(String value) {
        if (value == null) {
            return BASIC;
        }
        for (MembershipType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return BASIC;
    }
}
