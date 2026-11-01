package demo;

import engine.ProjectionImage;

public final class GrayscaleMapper {
    private final long min;
    private final long max;

    public GrayscaleMapper(long min, long max) {
        this.min = min;
        this.max = max;
    }

    public int toGray(long v) {
        if (max == min) {
            return 0;
        }
        long scaled = (255L * (v - min) + (max - min) / 2) / (max - min);
        if (scaled < 0) return 0;
        if (scaled > 255) return 255;
        return (int) scaled;
    }

    // Fills frame (row by row, size width*height) from the image.
    public void map(ProjectionImage image, byte[] frame) {
        int w = image.width();
        int h = image.height();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                frame[x + w * y] = (byte) toGray(image.get(x, y));
            }
        }
    }
}