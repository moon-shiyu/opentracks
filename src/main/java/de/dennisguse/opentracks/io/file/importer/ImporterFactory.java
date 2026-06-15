package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

/**
 * Import-side format dispatch.
 * <p>
 * Resolves the importer matching a file extension and runs it. This mirrors the export-side
 * dispatch in {@link TrackFileFormat#createTrackExporter}, keeping the supported extensions
 * ({@code gpx}, {@code kml}, {@code kmz}) and their importer wiring in a single place.
 */
public class ImporterFactory {

    private ImporterFactory() {
    }

    /**
     * Imports the file at {@code uri} using the importer matching {@code fileExtension}.
     *
     * @return the imported track ids (possibly empty), or {@code null} if {@code fileExtension} has no supported importer.
     */
    @Nullable
    public static List<Track.Id> importFile(@NonNull Context context, @NonNull TrackImporter trackImporter, @NonNull Uri uri, String fileExtension) throws IOException {
        if (TrackFileFormat.GPX.getExtension().equals(fileExtension)) {
            return new XMLImporter(new GPXTrackImporter(context, trackImporter)).importFile(context, uri);
        }
        if (TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.getExtension().equals(fileExtension)) {
            return new XMLImporter(new KMLTrackImporter(context, trackImporter)).importFile(context, uri);
        }
        if (TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES.getExtension().equals(fileExtension)) {
            return new KMZTrackImporter(context, trackImporter).importFile(uri);
        }
        return null;
    }
}
