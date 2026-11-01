package engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectorTest {

    @Test
    void constantVolumeGivesConstantPixels() {
        // a 10x10x10 cube, every voxel = 255 (-1 as a signed byte)
        byte[] data = new byte[10 * 10 * 10];
        java.util.Arrays.fill(data, (byte) 0xFF);
        Volume cube = new ByteVolume(10, 10, 10, data);

        // an isosceles triangle in the plane z = 5
        PlaneDefinition plane = new TrianglePlane(
                new Vec3(5, 0, 5), new Vec3(1, 6, 5), new Vec3(9, 6, 5));
        ProjectionImage img = new ProjectionImage(plane.width(), plane.height());

        Projector.project(cube, plane, img);

        boolean foundWritten = false;
        for (int y = 0; y < img.height(); y++) {
            for (int x = 0; x < img.width(); x++) {
                long v = img.get(x, y);
                assertTrue(v == 0 || v == 255, "pixel must be 0 or 255, got " + v);
                if (v == 255) foundWritten = true;
            }
        }
        assertTrue(foundWritten, "at least one pixel should be 255");
    }

    @Test
    void axisAlignedSliceReadsCorrectVoxels() {
        // value of voxel (x,y,z) = x + 10*y + 100*z, so every voxel is unique
        int s = 10;
        short[] data = new short[s * s * s];
        for (int z = 0; z < s; z++)
            for (int y = 0; y < s; y++)
                for (int x = 0; x < s; x++)
                    data[x + s * (y + s * z)] = (short) (x + 10 * y + 100 * z);
        Volume cube = new ShortVolume(s, s, s, data);

        // a rectangle (trapezoid with a1->a2 as long as b->c) in the plane z = 3
        // top side from (0,-1,3) to (7,-1,3), legs go in +y direction, length 5
        PlaneDefinition plane = new TrapezoidPlane(
                new Vec3(0, -1, 3), new Vec3(7, -1, 3),
                new Vec3(0, 4, 3), new Vec3(7, 4, 3));
        ProjectionImage img = new ProjectionImage(plane.width(), plane.height());

        Projector.project(cube, plane, img);

        // row y of the image is sampled at plane-y = y (the algorithm steps first)
        // image width is 7; the row has 8 samples, x = 0..7 (the last one is dropped)
        // pixel (x, row) should be voxel (x, row, 3)
        for (int row = 0; row < img.height(); row++) {
            for (int x = 0; x < img.width(); x++) {
                long expected = x + 10L * row + 300;
                assertEquals(expected, img.get(x, row),
                        "pixel (" + x + "," + row + ")");
            }
        }
    }
}