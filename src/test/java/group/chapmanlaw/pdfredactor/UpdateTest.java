package group.chapmanlaw.pdfredactor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateTest {

    @Test
    void detectsNewerRemoteVersion() {
        assertTrue(update.isRemoteVersionNewer("1.1.0", "1.2.0"));
        assertTrue(update.isRemoteVersionNewer("1.1.0", "2.0.0"));
    }

    @Test
    void ignoresSameOrOlderVersions() {
        assertFalse(update.isRemoteVersionNewer("1.1.0", "1.1.0"));
        assertFalse(update.isRemoteVersionNewer("1.2.0", "1.1.9"));
    }
}
