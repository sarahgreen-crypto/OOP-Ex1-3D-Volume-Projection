package demo;

import engine.PlaneDefinition;
import engine.ProjectionImage;
import engine.Projector;
import engine.TrapezoidPlane;
import engine.Vec3;
import engine.Volume;
import io.NrrdReader;
import io.ReadOptions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

// A small helper to look at one slice. Not part of the final demo.
public final class SliceToPng {

    public static void main(String[] args) throws Exception {
        Volume v = NrrdReader.read(Path.of("data", "MRHead.nrrd"), ReadOptions.allowSigned());

        // find min and max of the volume (for the gray scale)
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (int z = 0; z < v.sz(); z++) {
            for (int y = 0; y < v.sy(); y++) {
                for (int x = 0; x < v.sx(); x++) {
                    long value = v.getUnsigned(x, y, z);
                    if (value < min) min = value;
                    if (value > max) max = value;
                }
            }
        }
        System.out.println("min=" + min + " max=" + max);

        // a rectangle that covers the whole slice z = 65
        PlaneDefinition plane = new TrapezoidPlane(
                new Vec3(0, 0, 65), new Vec3(255, 0, 65),
                new Vec3(0, 255, 65), new Vec3(255, 255, 65));
        ProjectionImage img = new ProjectionImage(plane.width(), plane.height());
        Projector.project(v, plane, img);

        GrayscaleMapper mapper = new GrayscaleMapper(min, max);
        BufferedImage out = new BufferedImage(img.width(), img.height(), BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < img.height(); y++) {
            for (int x = 0; x < img.width(); x++) {
                int g = mapper.toGray(img.get(x, y));
                out.getRaster().setSample(x, y, 0, g);
            }
        }
        ImageIO.write(out, "png", Path.of("slice.png").toFile());
        System.out.println("Saved slice.png");
    }
}