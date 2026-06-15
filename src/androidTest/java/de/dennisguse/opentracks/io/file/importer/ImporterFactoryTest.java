package de.dennisguse.opentracks.io.file.importer;

import static org.junit.Assert.assertNull;

import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Distance;

/**
 * Tests the import-side format dispatch in {@link ImporterFactory}.
 * <p>
 * The supported extensions ({@code gpx}, {@code kml}, {@code kmz}) are covered end-to-end by
 * {@link GPXTrackImporterTest}, {@link KMLTrackImporterTest} and {@link ExportImportTest}; this
 * pins the behavior unique to the factory: unknown extensions are reported as unsupported.
 */
@RunWith(JUnit4.class)
public class ImporterFactoryTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    private TrackImporter trackImporter;

    @Before
    public void setUp() {
        trackImporter = new TrackImporter(context, new ContentProviderUtils(context), Distance.of(200), true);
    }

    /**
     * An unknown extension has no importer and must be reported as unsupported (null) rather than crash.
     */
    @SmallTest
    @Test
    public void unsupportedExtension_returnsNull() throws IOException {
        assertNull(ImporterFactory.importFile(context, trackImporter, Uri.parse("file:///dummy.xyz"), "xyz"));
    }
}
