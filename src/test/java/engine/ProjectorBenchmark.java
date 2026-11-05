package engine;

import java.util.Arrays;
import java.util.Random;

// Measures the speed of Projector.project (SRS NFR-P1). Run the main method.
public final class ProjectorBenchmark {

    private static final int SIZE = 500;
    private static final int WARM_UP = 1000;
    private static final int MEASURED = 1000;

    public static void main(String[] args) {
        byte[] data = new byte[SIZE * SIZE * SIZE];
        new Random(1).nextBytes(data);
        Volume volume = new ByteVolume(SIZE, SIZE, SIZE, data);

        double last = SIZE - 1;

        // 1. axis aligned: z = 250
        PlaneDefinition axis = new TrapezoidPlane(
                new Vec3(0, 0, 250), new Vec3(last, 0, 250),
                new Vec3(0, last, 250), new Vec3(last, last, 250));

        // 2. along the slowest memory axis: y = 250 (x and z change)
        PlaneDefinition slow = new TrapezoidPlane(
                new Vec3(0, 250, 0), new Vec3(last, 250, 0),
                new Vec3(0, 250, last), new Vec3(last, 250, last));

        // 3. oblique: 45 degrees around the x axis
        PlaneDefinition oblique = new TrapezoidPlane(
                new Vec3(0, 60, 60), new Vec3(last, 60, 60),
                new Vec3(0, 440, 440), new Vec3(last, 440, 440));

        System.out.println("Java " + System.getProperty("java.version")
                + ", " + System.getProperty("os.arch")
                + ", cores: " + Runtime.getRuntime().availableProcessors()
                + ", threads used by the engine: 1");
        System.out.println("Volume " + SIZE + "^3 bytes, warm-up " + WARM_UP
                + ", measured " + MEASURED);

        measure("axis aligned (z=250)", volume, axis);
        measure("slowest axis (y=250)", volume, slow);
        measure("oblique 45 degrees", volume, oblique);
    }

    private static void measure(String name, Volume volume, PlaneDefinition plane) {
        ProjectionImage image = new ProjectionImage(plane.width(), plane.height());
        long checksum = 0;

        for (int i = 0; i < WARM_UP; i++) {
            Projector.project(volume, plane, image);
            checksum += image.get(image.width() / 2, image.height() / 2);
        }

        long[] times = new long[MEASURED];
        for (int i = 0; i < MEASURED; i++) {
            long start = System.nanoTime();
            Projector.project(volume, plane, image);
            times[i] = System.nanoTime() - start;
            checksum += image.get(image.width() / 2, image.height() / 2);
        }

        Arrays.sort(times);
        long median = times[MEASURED / 2];
        long worst1Percent = times[(int) (MEASURED * 0.99)];
        System.out.printf("%-24s image %dx%d  median %.3f ms = %.0f FPS,"
                        + "  worst 1%% %.3f ms = %.0f FPS  (check %d)%n",
                name, image.width(), image.height(),
                median / 1e6, 1e9 / median,
                worst1Percent / 1e6, 1e9 / worst1Percent, checksum);
    }
}