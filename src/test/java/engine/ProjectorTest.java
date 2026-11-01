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
}