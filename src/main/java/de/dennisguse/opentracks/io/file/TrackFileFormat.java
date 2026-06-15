package de.dennisguse.opentracks.io.file;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.exporter.CSVTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.GPXTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.KMLTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.KMZTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;
import de.dennisguse.opentracks.io.file.importer.GPXTrackImporter;
import de.dennisguse.opentracks.io.file.importer.KMLTrackImporter;
import de.dennisguse.opentracks.io.file.importer.TrackImporter;
import de.dennisguse.opentracks.io.file.importer.XMLImporter;

/**
 * Definition of all possible track formats.
 * <p>
 * NOTE: The names of the entries are used in the user's settings.
 */
public enum TrackFileFormat {

    KML_WITH_TRACKDETAIL_AND_SENSORDATA("KML_WITH_TRACKDETAIL_AND_SENSORDATA") {
        @Override
        public TrackExporter createTrackExporter(@NonNull Context context, @NonNull ContentProviderUtils contentProviderUtils) {
            return new KMLTrackExporter(context, contentProviderUtils, false);
        }

        @Override
        public String getMimeType() {
            return "application/vnd.google-earth.kml+xml";
        }

        public String getExtension() {
            return "kml";
        }

        @Override
        public boolean supportsImport() {
            return true;
        }

        @Override
        public XMLImporter.TrackParser createTrackImporter(@NonNull Context context, @NonNull TrackImporter trackImporter) {
            return new KMLTrackImporter(context, trackImporter);
        }
    },

    @Deprecated //TODO Check if we really need this
    KMZ_WITH_TRACKDETAIL_AND_SENSORDATA("KMZ_WITH_TRACKDETAIL_AND_SENSORDATA") {

        private static final boolean exportPhotos = false;

        @Override
        public TrackExporter createTrackExporter(@NonNull Context context, @NonNull ContentProviderUtils contentProviderUtils) {
            KMLTrackExporter exporter = new KMLTrackExporter(context, contentProviderUtils, exportPhotos);
            return new KMZTrackExporter(context, contentProviderUtils, exporter, exportPhotos);
        }

        @Override
        public String getMimeType() {
            return "application/vnd.google-earth.kmz";
        }

        public String getExtension() {
            return "kmz";
        }

        @Override
        public boolean includesPhotos() {
            return exportPhotos;
        }
    },

    KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES("KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES") {

        private static final boolean exportPhotos = true;

        @Override
        public TrackExporter createTrackExporter(@NonNull Context context, @NonNull ContentProviderUtils contentProviderUtils) {
            KMLTrackExporter exporter = new KMLTrackExporter(context, contentProviderUtils, exportPhotos);
            return new KMZTrackExporter(context, contentProviderUtils, exporter, exportPhotos);
        }

        @Override
        public String getMimeType() {
            return KMZ_WITH_TRACKDETAIL_AND_SENSORDATA.getMimeType();
        }

        public String getExtension() {
            return "kmz";
        }

        @Override
        public boolean includesPhotos() {
            return exportPhotos;
        }

    },

    GPX("GPX") {
        @Override
        public TrackExporter createTrackExporter(@NonNull Context context, @NonNull ContentProviderUtils contentProviderUtils) {
            return new GPXTrackExporter(contentProviderUtils, context.getString(R.string.app_name));
        }

        @Override
        public String getMimeType() {
            return "application/gpx+xml";
        }

        public String getExtension() {
            return "gpx";
        }

        @Override
        public boolean supportsImport() {
            return true;
        }

        @Override
        public XMLImporter.TrackParser createTrackImporter(@NonNull Context context, @NonNull TrackImporter trackImporter) {
            return new GPXTrackImporter(context, trackImporter);
        }
    },

    CSV("CSV") {
        @Override
        public TrackExporter createTrackExporter(@NonNull Context context, @NonNull ContentProviderUtils contentProviderUtils) {
            return new CSVTrackExporter(contentProviderUtils);
        }

        @Override
        public String getMimeType() {
            return "text/csv";
        }

        @Override
        public String getExtension() {
            return "csv";
        }
    };

    private final String preferenceId;

    TrackFileFormat(String preferenceId) {
        this.preferenceId = preferenceId;
    }

    public static Map<String, String> toPreferenceIdLabelMap(final Resources resources, final TrackFileFormat... trackFileFormats) {
        Map<String, String> preferenceIdLabelMap = new LinkedHashMap<>(trackFileFormats.length);
        for (TrackFileFormat trackFileFormat : trackFileFormats) {
            String trackFileFormatUpperCase = trackFileFormat.getExtension().toUpperCase(Locale.US); //ASCII upper case
            int photoMessageId = trackFileFormat.includesPhotos() ? R.string.export_with_photos : R.string.export_without_photos;
            preferenceIdLabelMap.put(trackFileFormat.getPreferenceId(), String.format("%s (%s)", trackFileFormatUpperCase, resources.getString(photoMessageId)));
        }
        return preferenceIdLabelMap;
    }

    public static TrackFileFormat valueOfPreferenceId(final String preferenceId) {
        return Arrays.stream(values())
                .filter(trackFileFormat -> trackFileFormat.getPreferenceId().equals(preferenceId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the mime type for each format.
     */
    public abstract String getMimeType();

    /**
     * Creates a new track writer for the format.
     *
     * @param context the context
     */
    public abstract TrackExporter createTrackExporter(@NonNull Context context, @NonNull ContentProviderUtils contentProviderUtils);

    /**
     * Returns the file extension for each format.
     */
    public abstract String getExtension();

    /**
     * Returns whether the format supports photos.
     */
    public boolean includesPhotos() {
        return false;
    }

    /**
     * Returns whether this format supports importing track files.
     */
    public boolean supportsImport() {
        return false;
    }

    /**
     * Creates a track importer for this format.
     * Only valid for formats where {@link #supportsImport()} returns true.
     *
     * @param context       the context
     * @param trackImporter the shared track importer accumulator
     */
    public XMLImporter.TrackParser createTrackImporter(@NonNull Context context, @NonNull TrackImporter trackImporter) {
        return null;
    }

    /**
     * Looks up a track file format by extension that supports import.
     *
     * @param extension the file extension (e.g., "gpx", "kml")
     * @return the matching format, or null if none supports import for this extension
     */
    public static TrackFileFormat forImportExtension(String extension) {
        for (TrackFileFormat format : values()) {
            if (format.supportsImport() && format.getExtension().equals(extension)) {
                return format;
            }
        }
        return null;
    }

    /**
     * The identifier to be stored in the preferences.
     */
    public String getPreferenceId() {
        return preferenceId;
    }
}