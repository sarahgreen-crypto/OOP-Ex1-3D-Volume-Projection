package io;

import engine.PlaneDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlaneCsvReaderTest {

    @Test
    void readsHeaderTriangleAndTrapezoid() throws Exception {
        List<String> lines = List.of(
                "planes",
                "5,0,5, 1,6,5, 9,6,5",
                "",
                "0,-1,3, 7,-1,3, 0,4,3, 7,4,3");
        List<PlaneDefinition> planes = PlaneCsvReader.parse(lines);
        assertEquals(2, planes.size());
    }

    @Test
    void wrongNumberOfValuesNamesTheLine() {
        List<String> lines = List.of("1,2,3,4,5");
        CsvFormatException e = assertThrows(CsvFormatException.class,
                () -> PlaneCsvReader.parse(lines));
        assertTrue(e.getMessage().contains("Line 1"));
    }

    @Test
    void invalidPlaneNamesTheLine() {
        // legs of very different length: not isosceles
        List<String> lines = List.of("0,0,0, 0,10,0, 6,9,0");
        CsvFormatException e = assertThrows(CsvFormatException.class,
                () -> PlaneCsvReader.parse(lines));
        assertTrue(e.getMessage().contains("Line 1"));
    }

    @Test
    void emptyFileIsAnError() {
        assertThrows(CsvFormatException.class, () -> PlaneCsvReader.parse(List.of()));
    }
}