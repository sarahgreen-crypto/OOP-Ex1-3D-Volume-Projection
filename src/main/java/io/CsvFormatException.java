package io;

import java.io.IOException;

public class CsvFormatException extends IOException {
    public CsvFormatException(String message) {
        super(message);
    }
}