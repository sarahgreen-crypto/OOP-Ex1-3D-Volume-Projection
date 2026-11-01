package engine;

public final class TrianglePlane implements PlaneDefinition {
    private static final double EPS = 0.001;
    private static final double TINY = 1e-9;
    private static final int MAX_OUTPUT_DIM = 4096;

    private final int width;
    private final int height;
    private final double length;
    private final double sx, sy, sz;     // נקודת ההתחלה (a)
    private final double d1x, d1y, d1z;  // כיוון לכיוון b
    private final double d2x, d2y, d2z;  // כיוון לכיוון c


    public TrianglePlane(Vec3 a, Vec3 b, Vec3 c) {
        // 1. בדיקה שכל המספרים תקינים (לא NaN ולא אינסוף)
        checkFinite(a);
        checkFinite(b);
        checkFinite(c);

        // 2. חישוב המרחקים
        double dab = dist(a, b);
        double dac = dist(a, c);
        double dbc = dist(b, c);

        // 3. בדיקת "שווה שוקיים"
        if (Math.abs(dab - dac) >= EPS) {
            throw new PlaneDefinitionException(
                    "Not an isosceles triangle: dist(a,b)=" + dab + ", dist(a,c)=" + dac);
        }

        // 4. בדיקת אורכים לא אפסיים
        if (dab < TINY || dbc < TINY) {
            throw new PlaneDefinitionException("Triangle is too small or degenerate");
        }

        // 5. בדיקת נקודות על קו אחד (מכפלה וקטורית)
        double ux = b.x() - a.x(), uy = b.y() - a.y(), uz = b.z() - a.z();
        double vx = c.x() - a.x(), vy = c.y() - a.y(), vz = c.z() - a.z();
        double cx = uy * vz - uz * vy;
        double cy = uz * vx - ux * vz;
        double cz = ux * vy - uy * vx;
        if (Math.sqrt(cx * cx + cy * cy + cz * cz) < TINY) {
            throw new PlaneDefinitionException("The three points are on one line");
        }

        // 6. חישוב גודל התמונה
        double h = Math.ceil(dab);
        double w = Math.ceil(dbc);
        if (h > MAX_OUTPUT_DIM || w > MAX_OUTPUT_DIM) {
            throw new PlaneDefinitionException("Output image too large (max " + MAX_OUTPUT_DIM + ")");
        }
        this.height = Math.max(1, (int) h);
        this.width = Math.max(1, (int) w);
        this.length = dab;

        // 7. שמירת נקודת ההתחלה והכיוונים (וקטורים באורך 1)
        this.sx = a.x();
        this.sy = a.y();
        this.sz = a.z();
        this.d1x = ux / dab;
        this.d1y = uy / dab;
        this.d1z = uz / dab;
        this.d2x = vx / dac;
        this.d2y = vy / dac;
        this.d2z = vz / dac;
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
    public double start1X() { return sx; }
    @Override
    public double start1Y() { return sy; }
    @Override
    public double start1Z() { return sz; }

    // In a triangle both sides start at a.
    @Override
    public double start2X() { return sx; }
    @Override
    public double start2Y() { return sy; }
    @Override
    public double start2Z() { return sz; }

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