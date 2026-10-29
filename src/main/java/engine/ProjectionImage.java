package engine;

import java.util.Arrays;

public class ProjectionImage {
    private final int width;
    private final int height;
    private final long[] pixels;

    public ProjectionImage(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Image size must be at least 1x1");
        }
        this.width = width;
        this.height = height;
        this.pixels = new long[width * height];
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public long get(int x, int y) {
        return pixels[x + width * y];
    }

    public void set(int x, int y, long value) {
        pixels[x + width * y] = value;
    }

    public void clear() {
        Arrays.fill(pixels, 0L);
    }
}