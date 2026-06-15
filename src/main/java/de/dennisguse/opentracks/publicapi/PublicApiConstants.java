package de.dennisguse.opentracks.publicapi;

/**
 * Constants for the OpenTracks public API.
 * <p>
 * External apps use these string keys as Intent extras when calling
 * {@link StartRecording}, {@link StopRecording}, or {@link CreateMarker}
 * via explicit Intent.
 * <p>
 * See README_API.md for usage details.
 */
public final class PublicApiConstants {

    private PublicApiConstants() {
    }

    /** Track name (String). */
    public static final String EXTRA_TRACK_NAME = "TRACK_NAME";

    /** Track description (String). */
    public static final String EXTRA_TRACK_DESCRIPTION = "TRACK_DESCRIPTION";

    /** Localized activity type string, e.g. "Running" (String). */
    public static final String EXTRA_TRACK_ACTIVITY_TYPE_LOCALIZED = "TRACK_CATEGORY";

    /** Activity type ID, e.g. "running" (String). */
    public static final String EXTRA_TRACK_ACTIVITY_TYPE_ID = "TRACK_ICON";

    /** Target package for auto-starting a dashboard app (String). */
    public static final String EXTRA_STATS_TARGET_PACKAGE = "STATS_TARGET_PACKAGE";

    /** Target class for auto-starting a dashboard app (String). */
    public static final String EXTRA_STATS_TARGET_CLASS = "STATS_TARGET_CLASS";
}
