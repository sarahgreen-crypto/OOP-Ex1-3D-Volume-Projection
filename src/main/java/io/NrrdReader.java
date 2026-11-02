package io;

import engine.Volume;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class NrrdReader {
    
private NrrdReader() {
    // Private constructor to prevent instantiation
}
public static Volume read(Path path, ReadOptions options) throws IOException {
    try (InputStream inputStream = Files.newInputStream(path)) {
        return read(inputStream, options);
    }
}
}