package com.library.util;

import com.library.exception.InvalidInputException;

/**
 * Central validation, so the rules exist in one place and every service reports
 * failures in the same words.
 */
public final class Validator {

    private Validator() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String requireText(String value, String field) {
        if (isBlank(value)) {
            throw new InvalidInputException(field, "it cannot be empty");
        }
        return value.trim();
    }

    public static String requireName(String value, String field) {
        String trimmed = requireText(value, field);
        if (trimmed.length() < 2) {
            throw new InvalidInputException(field, "it must be at least 2 characters");
        }
        return trimmed;
    }

    /**
     * Checks the shape of an ISBN: 10 or 13 digits once separators are removed,
     * with an optional trailing X on a 10 digit number.
     *
     * @param isbn the candidate
     * @return the normalised ISBN, separators kept as supplied
     */
    public static String requireIsbn(String isbn) {
        String trimmed = requireText(isbn, "ISBN");
        String digits = trimmed.replace("-", "").replace(" ", "");
        if (digits.length() != 10 && digits.length() != 13) {
            throw new InvalidInputException("ISBN",
                    "'" + isbn + "' must contain 10 or 13 digits");
        }
        for (int i = 0; i < digits.length(); i++) {
            char character = digits.charAt(i);
            boolean lastCharMayBeX = i == digits.length() - 1 && Character.toUpperCase(character) == 'X';
            if (!Character.isDigit(character) && !lastCharMayBeX) {
                throw new InvalidInputException("ISBN", "'" + isbn + "' contains a non-digit");
            }
        }
        return trimmed;
    }

    public static String requireEmail(String email) {
        String trimmed = requireText(email, "email");
        int at = trimmed.indexOf('@');
        int dot = trimmed.lastIndexOf('.');
        if (at <= 0 || dot <= at + 1 || dot >= trimmed.length() - 1 || trimmed.contains(" ")) {
            throw new InvalidInputException("email", "'" + email + "' is not a valid address");
        }
        return trimmed;
    }

    public static int requireYear(int year) {
        int currentYear = java.time.Year.now().getValue();
        if (year < 1450 || year > currentYear + 1) {
            throw new InvalidInputException("publication year",
                    "it must be between 1450 and " + (currentYear + 1));
        }
        return year;
    }

    public static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new InvalidInputException(field, "it must be greater than zero");
        }
        return value;
    }

    public static int parseInt(String value, String field) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidInputException("'" + value + "' is not a whole number for " + field, e);
        } catch (NullPointerException e) {
            throw new InvalidInputException("No value was supplied for " + field, e);
        }
    }
}
