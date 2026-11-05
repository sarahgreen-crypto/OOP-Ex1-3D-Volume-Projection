package demo;

import engine.PlaneDefinition;
import engine.ProjectionImage;
import engine.Projector;
import engine.Volume;
import io.CsvFormatException;
import io.NrrdFormatException;
import io.NrrdReader;
import io.PlaneCsvReader;
import io.PlaneEntry;
import io.ReadOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class DemoRunner {

    private static final int FPS = 10;

    public int run(Path nrrdFilePath, Path csvFilePath, Path mp4FilePath) {
        try {
            Volume volume = NrrdReader.read(nrrdFilePath, ReadOptions.allowSigned());
            List<PlaneEntry> entries = PlaneCsvReader.read(csvFilePath);

            FrameSize frameSize = frameSizeFor(entries);
            ValueRange range = valueRangeOf(volume);
            GrayscaleMapper mapper = new GrayscaleMapper(range.min(), range.max());

            System.out.println("Volume: " + volume.sx() + " x " + volume.sy() + " x " + volume.sz());
            System.out.println("Planes: " + entries.size());
            System.out.println("Frame: " + frameSize.width() + " x " + frameSize.height());

            try (VideoWriter writer = new Mp4VideoWriter(mp4FilePath)) {
                writer.open(frameSize.width(), frameSize.height(), FPS);
                for (PlaneEntry entry : entries) {
                    if (entry.isValid()) {
                        writer.writeFrame(validFrame(volume, entry.plane(), mapper, frameSize));
                    } else {
                        System.err.println("Line " + entry.lineNumber() + ": " + entry.error()
                                + " (written as an error frame)");
                        writer.writeFrame(errorFrame(frameSize));
                    }
                }
            }
            System.out.println("Video written: " + mp4FilePath);
            return 0;
        } catch (NrrdFormatException e) {
            System.err.println("Problem with the NRRD file: " + e.getMessage());
            return 3;
        } catch (CsvFormatException e) {
            System.err.println("Problem with the CSV file: " + e.getMessage());
            return 4;
        } catch (IOException e) {
            System.err.println("Cannot read or write a file: " + e.getMessage());
            return 5;
        }
    }

    private record FrameSize(int width, int height) {
    }

    private record ValueRange(long min, long max) {
    }

    // Projects the plane, converts to gray and centres it on a black frame.
    private static byte[] validFrame(Volume volume, PlaneDefinition plane,
                                     GrayscaleMapper mapper, FrameSize size) {
        ProjectionImage image = new ProjectionImage(plane.width(), plane.height());
        Projector.project(volume, plane, image);
        byte[] gray = new byte[plane.width() * plane.height()];
        mapper.map(image, gray);

        byte[] frame = new byte[size.width() * size.height()];
        int offsetX = (size.width() - plane.width()) / 2;
        int offsetY = (size.height() - plane.height()) / 2;
        for (int y = 0; y < plane.height(); y++) {
            System.arraycopy(gray, y * plane.width(),
                    frame, (y + offsetY) * size.width() + offsetX, plane.width());
        }
        return frame;
    }

    // Black frame with a white X, so it is clearly marked as an error.
    private static byte[] errorFrame(FrameSize size) {
        byte[] frame = new byte[size.width() * size.height()];
        int w = size.width();
        int h = size.height();
        for (int x = 0; x < w; x++) {
            int y1 = (int) ((long) x * (h - 1) / Math.max(1, w - 1));
            frame[x + w * y1] = (byte) 255;
            frame[x + w * (h - 1 - y1)] = (byte) 255;
        }
        return frame;
    }

    // Only valid planes count; if there are none, a small default frame is used.
    private static FrameSize frameSizeFor(List<PlaneEntry> entries) {
        int maxWidth = 0;
        int maxHeight = 0;
        for (PlaneEntry entry : entries) {
            if (entry.isValid()) {
                maxWidth = Math.max(maxWidth, entry.plane().width());
                maxHeight = Math.max(maxHeight, entry.plane().height());
            }
        }
        if (maxWidth == 0) {
            maxWidth = 256;
            maxHeight = 256;
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