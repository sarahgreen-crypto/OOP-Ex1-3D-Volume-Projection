package engine;

public interface Volume {
    int sx();
    int sy();
    int sz();

    long getUnsigned(int x, int y, int z);
}