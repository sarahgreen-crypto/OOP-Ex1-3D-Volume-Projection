package io;

import engine.Volume;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NrrdReaderTest {

    private static final Path MRI = Path.of("data", "MRHead.nrrd");

    @Test
    void readsRealMriFile() throws Exception {
        Volume v = NrrdReader.read(MRI, ReadOptions.allowSigned());
        assertEquals(256, v.sx());
        assertEquals(256, v.sy());
        assertEquals(130, v.sz());
    }

    @Test
    void signedFileIsRejectedByDefault() {
        assertThrows(NrrdFormatException.class,
                () -> NrrdReader.read(MRI, ReadOptions.defaults()));
    }
}