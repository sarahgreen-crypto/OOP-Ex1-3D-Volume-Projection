package demo;

import java.io.IOException;

public interface VideoWriter extends AutoCloseable {

    /** Starts a video. Width and height must be even. */
    void open(int width, int height, int fps) throws IOException;

    /** Writes one frame: width*height gray values, row by row. */
    void writeFrame(byte[] gray) throws IOException;

    @Override
    void close() throws IOException;
}