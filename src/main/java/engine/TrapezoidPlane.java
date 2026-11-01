package engine;

public final class TrapezoidPlane implements PlaneDefinition {
    private static final double EPS = 0.001;
    private static final double TINY = 1e-9;
    private static final int MAX_OUTPUT_DIM = 4096;

    private final int width;
    private final int height;
    private final double length;
    private final double s1x, s1y, s1z;  // start of side 1 (a1)
    private final double s2x, s2y, s2z;  // start of side 2 (a2)
    private final double d1x, d1y, d1z;  // direction a1 -> b
    private final double d2x, d2y, d2z;  // direction a2 -> c


    public TrapezoidPlane(Vec3 a1, Vec3 a2, Vec3 b, Vec3 c) {
        checkFinite(a1);
        checkFinite(a2);
        checkFinite(b);
        checkFinite(c);

        double leg1 = dist(a1, b);
        double leg2 = dist(a2, c);
        double dbc = dist(b, c);
        double top = dist(a1, a2);

        // legs must be equal
        if (Math.abs(leg1 - leg2) >= EPS) {
            throw new PlaneDefinitionException(
                    "Legs differ: dist(a1,b)=" + leg1 + ", dist(a2,c)=" + leg2);
        }

        // top base must not be longer than bottom base
        if (top > dbc) {
            throw new PlaneDefinitionException(
                    "Top base dist(a1,a2)=" + top + " is longer than dist(b,c)=" + dbc);
        }

        // lengths must not be zero
        if (leg1 < TINY || leg2 < TINY || dbc < TINY) {
            throw new PlaneDefinitionException("Trapezoid is too small or degenerate");
        }

        // vectors from a1
        double ux = b.x() - a1.x(), uy = b.y() - a1.y(), uz = b.z() - a1.z();
        double vx = c.x() - a1.x(), vy = c.y() - a1.y(), vz = c.z() - a1.z();

        // normal of the plane through a1, b, c (cross product)
        double nx = uy * vz - uz * vy;
        double ny = uz * vx - ux * vz;
        double nz = ux * vy - uy * vx;
        double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nLen < TINY) {
            throw new PlaneDefinitionException("The points a1, b, c are on one line");
        }

        // a2 must lie on the plane through a1, b, c
        double distToPlane = Math.abs(
                (a2.x() - a1.x()) * nx + (a2.y() - a1.y()) * ny + (a2.z() - a1.z()) * nz) / nLen;
        if (distToPlane >= EPS) {
            throw new PlaneDefinitionException("The four points are not on one plane");
        }

        // image size
        double h = Math.ceil(leg1);
        double w = Math.ceil(dbc);
        if (h > MAX_OUTPUT_DIM || w > MAX_OUTPUT_DIM) {
            throw new PlaneDefinitionException("Output image too large (max " + MAX_OUTPUT_DIM + ")");
        }
        this.height = Math.max(1, (int) h);
        this.width = Math.max(1, (int) w);
        this.length = leg1;

        // start points and unit directions
        this.s1x = a1.x();
        this.s1y = a1.y();
        this.s1z = a1.z();
        this.s2x = a2.x();
        this.s2y = a2.y();
        this.s2z = a2.z();
        this.d1x = ux / leg1;
        this.d1y = uy / leg1;
        this.d1z = uz / leg1;
        this.d2x = (c.x() - a2.x()) / leg2;
        this.d2y = (c.y() - a2.y()) / leg2;
        this.d2z = (c.z() - a2.z()) / leg2;
    }

    private static void checkFinite(Vec3 p) {
        if (!Double.isFinite(p.x()) || !Double.isFinite(p.y()) || !Double.isFinite(p.z())) {
            throw new PlaneDefinitionException("A point has NaN or infinite coordinates");
        }
    }

    private static double dist(Vec3 p, Vec3 q) {
        double dx = p.x() - q.x();
        double dy = p.y() - q.y();
        double dz = p.z() - q.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public double length() {
        return length;
    }

    @Override
    public double start1X() { return s1x; }
    @Override
    public double start1Y() { return s1y; }
    @Override
    public double start1Z() { return s1z; }

    // Unlike the triangle, side 2 starts at a2, not at a1.
    @Override
    public double start2X() { return s2x; }
    @Override
    public double start2Y() { return s2y; }
    @Override
    public double start2Z() { return s2z; }

    @Override
    public double dir1X() { return d1x; }
    @Override
    public double dir1Y() { return d1y; }
    @Override
    public double dir1Z() { return d1z; }

    @Override
    public double dir2X() { return d2x; }
    @Override
    public double dir2Y() { return d2y; }
    @Override
    public double dir2Z() { return d2z; }
}