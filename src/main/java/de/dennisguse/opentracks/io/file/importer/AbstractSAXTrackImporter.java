package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;

import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;

/**
 * Base class for SAX-based track importers (GPX, KML).
 * <p>
 * Provides shared state and common logic:
 * <ul>
 *     <li>SAX content accumulation ({@link #content})</li>
 *     <li>Marker collection ({@link #markers})</li>
 *     <li>Zone offset tracking ({@link #zoneOffset})</li>
 *     <li>Error message formatting ({@link #createErrorMessage})</li>
 *     <li>File end handling ({@link #onFileEnd})</li>
 *     <li>{@link XMLImporter.TrackParser} interface implementation</li>
 * </ul>
 */
public abstract class AbstractSAXTrackImporter extends DefaultHandler implements XMLImporter.TrackParser {

    protected Locator locator;

    protected final Context context;
    protected final TrackImporter trackImporter;

    protected ZoneOffset zoneOffset;

    protected final ArrayList<Marker> markers = new ArrayList<>();

    // The current element content being accumulated by characters()
    protected String content = "";

    protected AbstractSAXTrackImporter(Context context, TrackImporter trackImporter) {
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
