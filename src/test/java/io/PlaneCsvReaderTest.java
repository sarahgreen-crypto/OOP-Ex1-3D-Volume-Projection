package io;

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
        List<PlaneEntry> entries = PlaneCsvReader.parse(lines);
        assertEquals(2, entries.size());
        assertTrue(entries.get(0).isValid());
        assertTrue(entries.get(1).isValid());
        assertEquals(2, entries.get(0).lineNumber());
        assertEquals(4, entries.get(1).lineNumber());
    }

    @Test
    void wrongNumberOfValuesBecomesInvalidEntry() throws Exception {
        List<PlaneEntry> entries = PlaneCsvReader.parse(List.of("1,2,3,4,5"));
        assertEquals(1, entries.size());
        assertFalse(entries.get(0).isValid());
        assertEquals(1, entries.get(0).lineNumber());
    }

    @Test
    void invalidPlaneDoesNotStopTheOthers() throws Exception {
        List<String> lines = List.of(
                "5,0,5, 1,6,5, 9,6,5",      // valid
                "0,0,0, 0,10,0, 6,9,0",     // not isosceles
                "0,-1,3, 7,-1,3, 0,4,3, 7,4,3"); // valid
        List<PlaneEntry> entries = PlaneCsvReader.parse(lines);
        assertEquals(3, entries.size());
        assertTrue(entries.get(0).isValid());
        assertFalse(entries.get(1).isValid());
        assertTrue(entries.get(2).isValid());
    }

    @Test
    void textInsteadOfNumberAfterFirstLineIsInvalidEntry() throws Exception {
        List<String> lines = List.of("5,0,5, 1,6,5, 9,6,5", "a,b,c");
        List<PlaneEntry> entries = PlaneCsvReader.parse(lines);
        assertEquals(2, entries.size());
        assertFalse(entries.get(1).isValid());
    }

    @Test
    void emptyFileIsAnError() {
        assertThrows(CsvFormatException.class, () -> PlaneCsvReader.parse(List.of()));
    }
}