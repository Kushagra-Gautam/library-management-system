package com.library.domain.enums;

/**
 * Subject classification used by the catalogue and by the recommendation engine.
 */
public enum Genre {

    FICTION("Fiction"),
    NON_FICTION("Non-fiction"),
    SCIENCE("Science"),
    TECHNOLOGY("Technology"),
    HISTORY("History"),
    BIOGRAPHY("Biography"),
    FANTASY("Fantasy"),
    MYSTERY("Mystery"),
    POETRY("Poetry"),
    CHILDREN("Children");

    private final String label;

    Genre(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Genre fromText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replace(' ', '_').replace('-', '_').toUpperCase();
        for (Genre genre : values()) {
            if (genre.name().equals(cleaned) || genre.label.equalsIgnoreCase(value.trim())) {
                return genre;
            }
        }
        return null;
    }
}
