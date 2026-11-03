package demo;

import org.jcodec.api.awt.AWTSequenceEncoder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public final class Mp4VideoWriter implements VideoWriter {

    private final Path file;
    private AWTSequenceEncoder encoder;
    private int width;
    private int height;

    public Mp4VideoWriter(Path file) {
        this.file = file;
    }

    @Override
    public void open(int width, int height, int fps) throws IOException {
        this.width = width;
        this.height = height;
        this.encoder = AWTSequenceEncoder.createSequenceEncoder(new File(file.toString()), fps);
    }

    @Override
    public void writeFrame(byte[] gray) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int g = gray[x + width * y] & 0xFF;
                image.setRGB(x, y, (g << 16) | (g << 8) | g);
            }
        }
        encoder.encodeImage(image);
    }

    @Override
    public void close() throws IOException {
        if (encoder != null) {
            encoder.finish();
        }
    }
}