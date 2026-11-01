package engine;

public final class Projector {

    private Projector() {}

    // Nearest-neighbour sample, as in SRS 2.5.
    static long sample(Volume m, double px, double py, double pz) {
        int i = (int) Math.floor(px + 0.5);
        int j = (int) Math.floor(py + 0.5);
        int k = (int) Math.floor(pz + 0.5);
        if (i < 0 || i >= m.sx() || j < 0 || j >= m.sy() || k < 0 || k >= m.sz()) {
            return 0;
        }
        return m.getUnsigned(i, j, k);
    }

    // Simple (slow) version, written straight from SRS Appendix A.
    public static void project(Volume m, PlaneDefinition p, ProjectionImage out) {
        final double tiny = 1e-9;
        final int w = p.width();
        final int h = p.height();

        out.clear();

        double a1x = p.start1X(), a1y = p.start1Y(), a1z = p.start1Z();
        double a2x = p.start2X(), a2y = p.start2Y(), a2z = p.start2Z();

        for (int y = 0; y < h; y++) {
            // step first, as in the assignment
            a1x += p.dir1X(); a1y += p.dir1Y(); a1z += p.dir1Z();
            a2x += p.dir2X(); a2y += p.dir2Y(); a2z += p.dir2Z();

            double ex = a2x - a1x, ey = a2y - a1y, ez = a2z - a1z;
            double d12 = Math.sqrt(ex * ex + ey * ey + ez * ez);

            double dpx = 0, dpy = 0, dpz = 0;
            if (d12 >= tiny) {
                dpx = ex / d12;
                dpy = ey / d12;
                dpz = ez / d12;
            }

            int x0 = (int) Math.floor(w / 2.0 - d12 / 2.0 + 0.5);
            double px = a1x, py = a1y, pz = a1z;
            int steps = (int) Math.floor(d12);

            for (int x1 = 0; x1 <= steps; x1++) {
                int x = x0 + x1;
                if (x >= 0 && x < w) {
                    out.set(x, y, sample(m, px, py, pz));
                }
                px += dpx; py += dpy; pz += dpz;
            }
        }
    }
}