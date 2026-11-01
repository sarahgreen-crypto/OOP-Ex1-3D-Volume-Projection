package io;

import engine.PlaneDefinition;
import engine.PlaneDefinitionException;
import engine.TrapezoidPlane;
import engine.TrianglePlane;
import engine.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PlaneCsvReader {

    private PlaneCsvReader() {}

    public static List<PlaneDefinition> read(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return parse(lines);
    }

    // Separate from read() so it can be tested without a file.
    static List<PlaneDefinition> parse(List<String> lines) throws CsvFormatException {
        List<PlaneDefinition> planes = new ArrayList<>();
        boolean sawContentLine = false;

        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split(",");
            double[] values = new double[parts.length];
            boolean numeric = true;
            for (int k = 0; k < parts.length; k++) {
                try {
                    values[k] = Double.parseDouble(parts[k].trim());
                } catch (NumberFormatException e) {
                    numeric = false;
                    break;
                }
            }

            // the first non-empty line may be a header
            if (!numeric && !sawContentLine) {
                sawContentLine = true;
                continue;
            }
            sawContentLine = true;

            if (!numeric) {
                throw new CsvFormatException("Line " + lineNumber + ": a value is not a number");
            }

            try {
                if (values.length == 9) {
                    planes.add(new TrianglePlane(
                            point(values, 0), point(values, 3), point(values, 6)));
                } else if (values.length == 12) {
                    planes.add(new TrapezoidPlane(
                            point(values, 0), point(values, 3),
                            point(values, 6), point(values, 9)));
                } else {
                    throw new CsvFormatException("Line " + lineNumber
                            + ": expected 9 or 12 numbers but found " + values.length);
                }
            } catch (PlaneDefinitionException e) {
                throw new CsvFormatException("Line " + lineNumber + ": " + e.getMessage());
            }
        }

        if (planes.isEmpty()) {
            throw new CsvFormatException("The CSV file contains no planes");
        }
        return planes;
    }

    private static Vec3 point(double[] v, int start) {
        return new Vec3(v[start], v[start + 1], v[start + 2]);
    }
}