package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;

import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;

/**
 * Shared SAX2 {@link DefaultHandler} base for the XML based {@link Track} importers (GPX and KML).
 * <p>
 * It bundles the plumbing that is identical across formats: tracking the SAX {@link Locator},
 * accumulating element text, collecting parsed {@link Marker}s, the
 * {@link XMLImporter.TrackParser} contract and a uniform parsing-error message.
 * <p>
 * Note: this is the SAX content handler base; {@link XMLImporter} is the driver that feeds it.
 */
abstract class XMLTrackImporter extends DefaultHandler implements XMLImporter.TrackParser {

    protected final Context context;
    protected final TrackImporter trackImporter;

    // Belongs to the current track
    protected final ArrayList<Marker> markers = new ArrayList<>();

    private Locator locator;

    // The current element content
    protected String content = "";

    XMLTrackImporter(Context context, TrackImporter trackImporter) {
        this.context = context;
        this.trackImporter = trackImporter;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        content += new String(ch, start, length);
    }

    protected String createErrorMessage(String message) {
        return String.format(Locale.US, "Parsing error at line: %d column: %d. %s", locator.getLineNumber(), locator.getColumnNumber(), message);
    }

    protected void onFileEnd() {
        trackImporter.addMarkers(markers);
        trackImporter.finish();
    }

    @Override
    public DefaultHandler getHandler() {
        return this;
    }

    @Override
    public List<Track.Id> getImportTrackIds() {
        return trackImporter.getTrackIds();
    }

    @Override
    public void cleanImport() {
        trackImporter.cleanImport();
    }
}
