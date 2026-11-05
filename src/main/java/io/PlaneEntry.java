package io;

import engine.PlaneDefinition;

/** The result of one CSV line: either a valid plane or an error message. */
public final class PlaneEntry {
    private final int lineNumber;
    private final PlaneDefinition plane;   // null if the line is bad
    private final String error;            // null if the line is good

    private PlaneEntry(int lineNumber, PlaneDefinition plane, String error) {
        this.lineNumber = lineNumber;
        this.plane = plane;
        this.error = error;
    }

    public static PlaneEntry valid(int lineNumber, PlaneDefinition plane) {
        return new PlaneEntry(lineNumber, plane, null);
    }

    public static PlaneEntry invalid(int lineNumber, String error) {
        return new PlaneEntry(lineNumber, null, error);
    }

    public boolean isValid() {
        return plane != null;
    }

    public int lineNumber() {
        return lineNumber;
    }

    /** Only call when isValid() is true. */
    public PlaneDefinition plane() {
        return plane;
    }

    /** Only call when isValid() is false. */
    public String error() {
        return error;
    }
}