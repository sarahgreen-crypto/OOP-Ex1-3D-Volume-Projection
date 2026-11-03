package io;

import engine.ByteVolume;
import engine.IntVolume;
import engine.ShortVolume;
import engine.Volume;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

/**
 * Reads a 3D NRRD file and builds a {@link Volume}.
 *
 * <p>Supported: {@code dimension: 3}; types uint8, uint16 and uint32 (and their other NRRD
 * names); signed types only with {@link ReadOptions#allowSigned()}; encoding raw or gzip;
 * endian little or big. The first axis (x) changes fastest in the file.</p>
 *
 * <p>Every problem with the file is reported as a {@link NrrdFormatException} with a message that
 * says what is wrong. A very large volume needs enough Java heap memory ({@code -Xmx}).</p>
 */
public final class NrrdReader {

    /** The largest allowed size on each axis: the assignment says size is below 2^10. */
    private static final int MAX_SIZE = 1023;

    /** How many bytes are read from the file at one time. A multiple of 4. */
    private static final int CHUNK_BYTES = 1 << 16;

    /** Longest allowed header line, to stop on files that are not NRRD files. */
    private static final int MAX_LINE_BYTES = 1 << 20;

    private NrrdReader() {
    }

    /** The supported voxel types. */
    private enum Kind {
        UINT8(1, false), INT8(1, true),
        UINT16(2, false), INT16(2, true),
        UINT32(4, false), INT32(4, true);

        final int bytes;
        final boolean signed;

        Kind(int bytes, boolean signed) {
            this.bytes = bytes;
            this.signed = signed;
        }
    }

    /**
     * Reads a file.
     *
     * @throws NrrdFormatException if the file is not a valid or supported NRRD file
     * @throws IOException         if the file cannot be read, or there is not enough memory
     */
    public static Volume read(Path path, ReadOptions options) throws IOException {
        Objects.requireNonNull(path, "path");
        try (InputStream in = Files.newInputStream(path)) {
            return read(in, options);
        }
    }

    /**
     * Reads from a stream that starts at the first byte of the NRRD file. The stream is not closed.
     *
     * @throws NrrdFormatException if the data is not a valid or supported NRRD file
     * @throws IOException         if reading fails, or there is not enough memory
     */
    public static Volume read(InputStream stream, ReadOptions options) throws IOException {
        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(options, "options");
        InputStream in = new BufferedInputStream(stream, CHUNK_BYTES);

        Map<String, String> header = readHeader(in);

        if (header.containsKey("data file") || header.containsKey("datafile")) {
            throw new NrrdFormatException(
                    "Detached data files (.nhdr) are not supported; use a single .nrrd file");
        }
        int dimension = parseInt(require(header, "dimension"), "dimension");
        if (dimension != 3) {
            throw new NrrdFormatException("Only 3D files are supported, but dimension is " + dimension);
        }
        int[] sizes = parseSizes(require(header, "sizes"));
        Kind kind = parseType(require(header, "type"));
        boolean gzip = parseEncoding(require(header, "encoding"));
        ByteOrder order = ByteOrder.BIG_ENDIAN;
        if (kind.bytes > 1) {
            order = parseEndian(header.get("endian"));
        }
        if (kind.signed && !options.isSignedAllowed()) {
            throw new NrrdFormatException("The type '" + header.get("type")
                    + "' is signed. Signed types are rejected unless the option"
                    + " ReadOptions.allowSigned() is used");
        }

        int sx = sizes[0];
        int sy = sizes[1];
        int sz = sizes[2];
        int count = (int) ((long) sx * sy * sz); // at most 1023^3, which fits in an int

        InputStream dataIn = in;
        if (gzip) {
            try {
                dataIn = new GZIPInputStream(in, CHUNK_BYTES);
            } catch (EOFException | ZipException e) {
                throw new NrrdFormatException("The compressed data is not valid gzip: " + describe(e));
            }
        }

        try {
            Volume volume = readData(dataIn, kind, order, sx, sy, sz, count);
            if (dataIn.read() != -1) {
                throw new NrrdFormatException("The file contains more data than sizes "
                        + sx + " " + sy + " " + sz + " need");
            }
            return volume;
        } catch (EOFException | ZipException e) {
            throw new NrrdFormatException("The data is truncated or corrupt: " + describe(e));
        } catch (OutOfMemoryError e) {
            throw new IOException("Not enough memory for " + count + " voxels of " + kind.bytes
                    + " byte(s); start Java with a larger -Xmx", e);
        }
    }

    // ---------------------------------------------------------------- header

    private static Map<String, String> readHeader(InputStream in) throws IOException {
        String magic = readLine(in);
        if (magic == null || !magic.startsWith("NRRD000")) {
            throw new NrrdFormatException("This is not an NRRD file: the first line must start with NRRD000");
        }
        Map<String, String> fields = new HashMap<>();
        while (true) {
            String line = readLine(in);
            if (line == null) {
                throw new NrrdFormatException("The file ends inside the header (no empty line after it)");
            }
            if (line.isEmpty()) {
                return fields; // an empty line ends the header
            }
            if (line.startsWith("#")) {
                continue; // comment
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                throw new NrrdFormatException("Bad header line (no colon): " + line);
            }
            if (colon + 1 < line.length() && line.charAt(colon + 1) == '=') {
                continue; // a key:=value pair of the file's author; not needed here
            }
            String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            fields.put(key, value);
        }
    }

    /** Reads one line (up to "\n"; a "\r" before it is removed). Returns null at the end of the stream. */
    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream lineBytes = new ByteArrayOutputStream();
        int currentByte = in.read();
        if (currentByte == -1) {
            return null;
        }
        while (currentByte != -1 && currentByte != '\n') {
            lineBytes.write(currentByte);
            if (lineBytes.size() > MAX_LINE_BYTES) {
                throw new NrrdFormatException("A header line is much too long; this is probably not an NRRD file");
            }
            currentByte = in.read();
        }
        String line = lineBytes.toString(StandardCharsets.ISO_8859_1);
        if (line.endsWith("\r")) {
            line = line.substring(0, line.length() - 1);
        }
        return line;
    }

    private static String require(Map<String, String> header, String key) throws NrrdFormatException {
        String value = header.get(key);
        if (value == null) {
            throw new NrrdFormatException("The header has no '" + key + "' field");
        }
        return value;
    }

    private static int parseInt(String text, String what) throws NrrdFormatException {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new NrrdFormatException("'" + what + "' is not a whole number: " + text);
        }
    }

    private static int[] parseSizes(String text) throws NrrdFormatException {
        String[] parts = text.trim().split("\\s+");
        if (parts.length != 3) {
            throw new NrrdFormatException("'sizes' must have 3 numbers but has " + parts.length + ": " + text);
        }
        int[] sizes = new int[3];
        for (int i = 0; i < 3; i++) {
            sizes[i] = parseInt(parts[i], "sizes");
            if (sizes[i] < 1 || sizes[i] > MAX_SIZE) {
                throw new NrrdFormatException("Size " + sizes[i] + " is outside the allowed range [1, "
                        + MAX_SIZE + "]");
            }
        }
        return sizes;
    }

    private static Kind parseType(String text) throws NrrdFormatException {
        String type = text.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        switch (type) {
            case "uchar": case "unsigned char": case "uint8": case "uint8_t":
                return Kind.UINT8;
            case "signed char": case "int8": case "int8_t":
                return Kind.INT8;
            case "ushort": case "unsigned short": case "unsigned short int": case "uint16": case "uint16_t":
                return Kind.UINT16;
            case "short": case "short int": case "signed short": case "signed short int":
            case "int16": case "int16_t":
                return Kind.INT16;
            case "uint": case "unsigned int": case "uint32": case "uint32_t":
                return Kind.UINT32;
            case "int": case "signed int": case "int32": case "int32_t":
                return Kind.INT32;
            default:
                throw new NrrdFormatException("Unsupported type '" + text
                        + "'. Supported: 8, 16 and 32-bit whole numbers");
        }
    }

    /** Returns true for gzip and false for raw. */
    private static boolean parseEncoding(String text) throws NrrdFormatException {
        String encoding = text.trim().toLowerCase(Locale.ROOT);
        switch (encoding) {
            case "raw":
                return false;
            case "gzip": case "gz":
                return true;
            default:
                throw new NrrdFormatException("Unsupported encoding '" + text + "'. Supported: raw, gzip");
        }
    }

    private static ByteOrder parseEndian(String text) throws NrrdFormatException {
        if (text == null) {
            throw new NrrdFormatException("The header has no 'endian' field, which is needed for 16 and 32-bit data");
        }
        switch (text.trim().toLowerCase(Locale.ROOT)) {
            case "little":
                return ByteOrder.LITTLE_ENDIAN;
            case "big":
                return ByteOrder.BIG_ENDIAN;
            default:
                throw new NrrdFormatException("'endian' must be little or big, but is '" + text + "'");
        }
    }

    // ------------------------------------------------------------------ data

    private static Volume readData(InputStream in, Kind kind, ByteOrder order,
                                   int sx, int sy, int sz, int count) throws IOException {
        byte[] chunk = new byte[CHUNK_BYTES];
        switch (kind.bytes) {
            case 1: {
                byte[] data = new byte[count];
                readFully(in, data, 0, count);
                if (kind.signed) {
                    for (int i = 0; i < count; i++) {
                        if (data[i] < 0) {
                            throw negative(i, data[i]);
                        }
                    }
                }
                return new ByteVolume(sx, sy, sz, data);
            }
            case 2: {
                short[] data = new short[count];
                int perChunk = CHUNK_BYTES / 2;
                for (int done = 0; done < count; done += perChunk) {
                    int voxelsInChunk = Math.min(perChunk, count - done);
                    readFully(in, chunk, 0, voxelsInChunk * 2);
                    ByteBuffer.wrap(chunk, 0, voxelsInChunk * 2).order(order).asShortBuffer().get(data, done, voxelsInChunk);
                }
                if (kind.signed) {
                    for (int i = 0; i < count; i++) {
                        if (data[i] < 0) {
                            throw negative(i, data[i]);
                        }
                    }
                }
                return new ShortVolume(sx, sy, sz, data);
            }
            default: {
                int[] data = new int[count];
                int perChunk = CHUNK_BYTES / 4;
                for (int done = 0; done < count; done += perChunk) {
                    int voxelsInChunk = Math.min(perChunk, count - done);
                    readFully(in, chunk, 0, voxelsInChunk * 4);
                    ByteBuffer.wrap(chunk, 0, voxelsInChunk * 4).order(order).asIntBuffer().get(data, done, voxelsInChunk);
                }
                if (kind.signed) {
                    for (int i = 0; i < count; i++) {
                        if (data[i] < 0) {
                            throw negative(i, data[i]);
                        }
                    }
                }
                return new IntVolume(sx, sy, sz, data);
            }
        }
    }

    private static NrrdFormatException negative(int index, long value) {
        return new NrrdFormatException("The file has a signed type and voxel number " + index
                + " is negative (" + value + "), so it cannot be read as unsigned");
    }

    private static String describe(IOException e) {
        return e.getMessage() == null ? "unexpected end of the data" : e.getMessage();
    }

    private static void readFully(InputStream in, byte[] buffer, int offset, int length) throws IOException {
        int done = 0;
        while (done < length) {
            int bytesRead = in.read(buffer, offset + done, length - done);
            if (bytesRead < 0) {
                throw new NrrdFormatException("The data ends too early: the sizes in the header need more bytes");
            }
            done += bytesRead;
        }
    }
}
