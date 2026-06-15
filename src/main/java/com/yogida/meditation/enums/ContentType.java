package com.yogida.meditation.enums;

/**
 * Defines content types that can be saved as user favourites.
 * Backend-owned: these values drive favourite creation, lookup, and calculation logic.
 */
public enum ContentType {
    MEDIA;

    /**
     * Get the string value for database storage and API serialization.
     */
    public String value() {
        return this.name();
    }

    /**
     * Parse a string value to ContentType enum.
     * Throws IllegalArgumentException if the value is not recognized.
     */
    public static ContentType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ContentType value cannot be null");
        }
        return ContentType.valueOf(value.toUpperCase());
    }
}
