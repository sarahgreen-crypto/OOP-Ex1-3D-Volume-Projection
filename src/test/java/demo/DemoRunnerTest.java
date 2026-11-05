package demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoRunnerTest {

    private static final int SIZE = 20;

    // JUnit creates a temporary folder for each test and deletes it afterwards.
    @TempDir
    Path tempDir;

    // Writes a small raw NRRD file (20 x 20 x 20, 8 bit) with changing values.
    private Path writeSmallNrrd() throws IOException {
        String header = "NRRD0004\n"
                + "type: uchar\n"
                + "dimension: 3\n"
                + "sizes: " + SIZE + " " + SIZE + " " + SIZE + "\n"
                + "encoding: raw\n"
                + "\n";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(header.getBytes(StandardCharsets.US_ASCII));
        for (int i = 0; i < SIZE * SIZE * SIZE; i++) {
            out.write(i % 251);
        }
        Path file = tempDir.resolve("small.nrrd");
        Files.write(file, out.toByteArray());
        return file;
    }

    private Path writeCsv(String... lines) throws IOException {
        Path file = tempDir.resolve("planes.csv");
        Files.write(file, List.of(lines));
        return file;
    }

    @Test
    void validAndInvalidPlanesGiveVideo() throws IOException {
        Path nrrd = writeSmallNrrd();
        Path csv = writeCsv(
                "planes",
                "10,2,10,2,17,10,18,17,10",          // triangle in z = 10
                "0,0,10,19,0,10,0,19,10,19,19,10",   // rectangle in z = 10
                "0,0,0,0,10,0,6,9,0");               // not isosceles: error frame
        Path mp4 = tempDir.resolve("out.mp4");

        int exitCode = new DemoRunner().run(nrrd, csv, mp4);

        assertEquals(0, exitCode);
        assertTrue(Files.exists(mp4), "the video file must exist");
        assertTrue(Files.size(mp4) > 0, "the video file must not be empty");
    }

    @Test
    void missingNrrdFileGivesExitCode5() throws IOException {
        Path csv = writeCsv("planes", "0,0,10,19,0,10,0,19,10,19,19,10");
        int exitCode = new DemoRunner().run(
                tempDir.resolve("missing.nrrd"), csv, tempDir.resolve("out.mp4"));
        assertEquals(5, exitCode);
    }

    @Test
    void notAnNrrdFileGivesExitCode3() throws IOException {
        Path fake = tempDir.resolve("fake.nrrd");
        Files.writeString(fake, "this is not an nrrd file\n");
        Path csv = writeCsv("planes", "0,0,10,19,0,10,0,19,10,19,19,10");
        int exitCode = new DemoRunner().run(fake, csv, tempDir.resolve("out.mp4"));
        assertEquals(3, exitCode);
    }

    @Test
    void emptyCsvGivesExitCode4() throws IOException {
        Path nrrd = writeSmallNrrd();
        Path csv = writeCsv();   // an empty file
        int exitCode = new DemoRunner().run(nrrd, csv, tempDir.resolve("out.mp4"));
        assertEquals(4, exitCode);
    }
}