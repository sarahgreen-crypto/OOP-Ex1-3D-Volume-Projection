package engine;

public final class ShortVolume implements Volume {
    private final int sx;
    private final int sy;
    private final int sz;
    private final short[] data;

    public ShortVolume(int sx, int sy, int sz, short[] data) {
        if (sx < 1 || sy < 1 || sz < 1) {
            throw new IllegalArgumentException("Volume sizes must be at least 1");
        }
        if (data.length != (long) sx * sy * sz) {
            throw new IllegalArgumentException("Data length does not match sx*sy*sz");
        }
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.data = data;
    }

    @Override
    public int sx() { return sx; }

    @Override
    public int sy() { return sy; }

    @Override
    public int sz() { return sz; }

    @Override
    public long getUnsigned(int x, int y, int z) {
        return Short.toUnsignedInt(data[x + sx * (y + sy * z)]);
    }
}