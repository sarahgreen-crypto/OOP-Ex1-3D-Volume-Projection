package demo;

import engine.PlaneDefinition;
import engine.Volume;
import io.CsvFormatException;
import io.NrrdFormatException;
import io.NrrdReader;
import io.PlaneCsvReader;
import io.ReadOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class DemoRunner {

    public int run(Path nrrdFilePath, Path csvFilePath, Path mp4FilePath) {
        try {
            Volume volume = NrrdReader.read(nrrdFilePath, ReadOptions.allowSigned());
            List<PlaneDefinition> planes = PlaneCsvReader.read(csvFilePath);

            System.out.println("Volume: " + volume.sx() + " x " + volume.sy() + " x " + volume.sz());
            System.out.println("Planes: " + planes.size());
                        FrameSize frameSize = frameSizeFor(planes);
            ValueRange range = valueRangeOf(volume);
            System.out.println("Frame: " + frameSize.width() + " x " + frameSize.height());
            System.out.println("Gray range: " + range.min() + " .. " + range.max());
            return 0;
        } catch (NrrdFormatException e) {
            System.err.println("Problem with the NRRD file: " + e.getMessage());
            return 3;
        } catch (CsvFormatException e) {
            System.err.println("Problem with the CSV file: " + e.getMessage());
            return 4;
        } catch (IOException e) {
            System.err.println("Cannot read a file: " + e.getMessage());
            return 5;
        }
    }
        private record FrameSize(int width, int height) {
    }

    private record ValueRange(long min, long max) {
    }

    private static FrameSize frameSizeFor(List<PlaneDefinition> planes) {
        int maxWidth = 0;
        int maxHeight = 0;
        for (PlaneDefinition plane : planes) {
            maxWidth = Math.max(maxWidth, plane.width());
            maxHeight = Math.max(maxHeight, plane.height());
        }
        return new FrameSize(roundUpToEven(maxWidth), roundUpToEven(maxHeight));
    }

    private static int roundUpToEven(int value) {
        return value % 2 == 0 ? value : value + 1;
    }

    private static ValueRange valueRangeOf(Volume volume) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (int z = 0; z < volume.sz(); z++) {
            for (int y = 0; y < volume.sy(); y++) {
                for (int x = 0; x < volume.sx(); x++) {
                    long value = volume.getUnsigned(x, y, z);
                    if (value < min) {
                        min = value;
                    }
                    if (value > max) {
                        max = value;
                    }
                }
            }
        }
        return new ValueRange(min, max);
    }
}