package de.dennisguse.opentracks.io.file;

/**
 * Constants shared between KML import and export.
 * <p>
 * These define the extended data type names used in KML 2.3 {@code <SimpleArrayData>}
 * and {@code <Data>} elements for OpenTracks custom track point and track metadata.
 */
public final class KMLConstants {

    private KMLConstants() {}

    // Marker style identifier
    public static final String MARKER_STYLE = "waypoint";

    // Track-level extended data
    public static final String EXTENDED_DATA_TYPE_LOCALIZED = "type";
    public static final String EXTENDED_DATA_ACTIVITY_TYPE = "activityType";

    // TrackPoint-level extended data (SimpleArrayData names)
    public static final String EXTENDED_DATA_TYPE_TRACKPOINT = "trackpoint_type";
    public static final String EXTENDED_DATA_TYPE_SPEED = "speed";
    public static final String EXTENDED_DATA_TYPE_DISTANCE = "distance";
    public static final String EXTENDED_DATA_TYPE_CADENCE = "cadence";
    public static final String EXTENDED_DATA_TYPE_HEARTRATE = "heartrate";
    public static final String EXTENDED_DATA_TYPE_POWER = "power";
    public static final String EXTENDED_DATA_TYPE_ALTITUDE_GAIN = "elevation_gain";
    public static final String EXTENDED_DATA_TYPE_ALTITUDE_LOSS = "elevation_loss";
    public static final String EXTENDED_DATA_TYPE_ACCURACY_HORIZONTAL = "accuracy_horizontal";
    public static final String EXTENDED_DATA_TYPE_ACCURACY_VERTICAL = "accuracy_vertical";
}
