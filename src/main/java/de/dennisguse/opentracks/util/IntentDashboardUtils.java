package de.dennisguse.opentracks.util;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.tables.MarkerColumns;
import de.dennisguse.opentracks.data.tables.TrackPointsColumns;
import de.dennisguse.opentracks.data.tables.TracksColumns;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/**
 * Create an {@link Intent} to request showing tracks on a Map or a Dashboard.
 * The receiving {@link android.app.Activity} gets temporary access to the {@link TracksColumns} and the {@link TrackPointsColumns} (incl. update).
 */
public class IntentDashboardUtils {

    private static final String TAG = IntentDashboardUtils.class.getSimpleName();

    static final String ACTION_DASHBOARD = "Intent.OpenTracks-Dashboard";

    private static final String ACTION_DASHBOARD_PAYLOAD = ACTION_DASHBOARD + ".Payload";

    /**
     * Assume "v1" if not present.
     */
    static final String EXTRAS_PROTOCOL_VERSION = "PROTOCOL_VERSION";

    /**
     * version 1: the initial version.
     * version 2: replaced pause/resume trackpoints for track segmentation (lat=100 / lat=200) by TrackPoint.Type.
     */
    static final int CURRENT_VERSION = 2;

    static final String EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK = "EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK";
    static final String EXTRAS_SHOULD_KEEP_SCREEN_ON = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    static final String EXTRAS_SHOW_WHEN_LOCKED = "EXTRAS_SHOW_WHEN_LOCKED";
    static final String EXTRAS_SHOW_FULLSCREEN = "EXTRAS_SHOULD_FULLSCREEN";

    private static final int TRACK_URI_INDEX = 0;
    private static final int TRACKPOINTS_URI_INDEX = 1;
    private static final int MARKERS_URI_INDEX = 2;
    private static final int NONE_SELECTED = -1;

    private IntentDashboardUtils() {
    }

    /**
     * Send intent to show tracks on a map (needs an another app) as resource URIs.
     *
     * @param context  the context
     * @param isRecording are we currently recording?
     * @param trackIds the track ids
     */
    public static void showTrackOnMap(Context context, boolean isRecording, Track.Id... trackIds) {
        startDashboard(context, isRecording, null, null, trackIds);
    }

    /**
     * Send intent to show tracks on a map (needs an another app) as resource URIs.
     * By providing a targetPackage and targetClass an explicit intent can be sent,
     * thus bypassing the need for the user to select an app.
     *
     * @param context the context
     * @param isRecording are we currently recording?
     * @param targetPackage the target package
     * @param targetClass the target class
     * @param trackIds the track ids
     */
    public static void startDashboard(Context context, boolean isRecording, @Nullable String targetPackage, @Nullable String targetClass, Track.Id... trackIds) {
        if (trackIds.length == 0) {
            return;
        }

        Intent intent = buildDashboardIntent(context, isRecording, targetPackage, targetClass, trackIds);

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Dashboard not installed; cannot start it.");
            Toast.makeText(context, R.string.show_on_dashboard_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Builds the dashboard Intent without starting it.
     * <p>
     * The Intent includes:
     * <ul>
     *     <li>Content URIs for tracks, track points, and markers (via ClipData)</li>
     *     <li>Temporary read permission ({@link Intent#FLAG_GRANT_READ_URI_PERMISSION})</li>
     *     <li>Protocol version and recording state extras</li>
     * </ul>
     *
     * @param context       the context for reading preferences
     * @param isRecording   whether a track is currently being recorded
     * @param targetPackage optional target package for explicit intent
     * @param targetClass   optional target class for explicit intent
     * @param trackIds      the track IDs to include
     * @return the constructed dashboard Intent
     */
    @VisibleForTesting
    static Intent buildDashboardIntent(Context context, boolean isRecording,
            @Nullable String targetPackage, @Nullable String targetClass,
            Track.Id... trackIds) {
        String trackIdList = ContentProviderUtils.formatIdListForUri(trackIds);

        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(TRACK_URI_INDEX, Uri.withAppendedPath(TracksColumns.CONTENT_URI, trackIdList));
        uris.add(TRACKPOINTS_URI_INDEX, Uri.withAppendedPath(TrackPointsColumns.CONTENT_URI_BY_TRACKID, trackIdList));
        uris.add(MARKERS_URI_INDEX, Uri.withAppendedPath(MarkerColumns.CONTENT_URI_BY_TRACKID, trackIdList));

        Intent intent = new Intent(ACTION_DASHBOARD);
        intent.putExtra(EXTRAS_PROTOCOL_VERSION, CURRENT_VERSION);

        intent.putParcelableArrayListExtra(ACTION_DASHBOARD_PAYLOAD, uris);

        intent.putExtra(EXTRAS_SHOULD_KEEP_SCREEN_ON, PreferencesUtils.shouldKeepScreenOn());
        intent.putExtra(EXTRAS_SHOW_WHEN_LOCKED, PreferencesUtils.shouldShowStatsOnLockscreen());
        intent.putExtra(EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK, isRecording);
        if (isRecording) {
            intent.putExtra(EXTRAS_SHOW_FULLSCREEN, PreferencesUtils.shouldUseFullscreen());
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ClipData clipData = ClipData.newRawUri(null, uris.get(TRACK_URI_INDEX));
        clipData.addItem(new ClipData.Item(uris.get(TRACKPOINTS_URI_INDEX)));
        clipData.addItem(new ClipData.Item(uris.get(MARKERS_URI_INDEX)));
        intent.setClipData(clipData);

        if (targetPackage != null && targetClass != null) {
            Log.i(TAG, "Starting dashboard activity with explicit intent (package=" + targetPackage + ", class=" + targetClass + ")");
            intent.setClassName(targetPackage, targetClass);
        } else {
            Log.i(TAG, "Starting dashboard activity with generic intent (package=" + targetPackage + ", class=" + targetClass + ")");
        }

        return intent;
    }
}
