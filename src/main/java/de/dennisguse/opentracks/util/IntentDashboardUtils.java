package de.dennisguse.opentracks.util;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.tables.MarkerColumns;
import de.dennisguse.opentracks.data.tables.TrackPointsColumns;
import de.dennisguse.opentracks.data.tables.TracksColumns;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/**
 * Create an {@link Intent} to request showing tracks on a Map or a Dashboard.
 * <p>
 * The receiving {@link android.app.Activity} is granted <em>temporary, read-only</em> access to the
 * {@link TracksColumns}, {@link TrackPointsColumns} and {@link MarkerColumns} of the selected tracks.
 * Access is granted exclusively via {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} (never write access)
 * and is automatically revoked by the system. This mirrors the privacy contract documented in
 * {@code README_API.md} ("access is only granted temporarily and automatically revoked", "no write
 * access is possible").
 */
public class IntentDashboardUtils {

    private static final String TAG = IntentDashboardUtils.class.getSimpleName();

    private static final String ACTION_DASHBOARD = "Intent.OpenTracks-Dashboard";

    private static final String ACTION_DASHBOARD_PAYLOAD = ACTION_DASHBOARD + ".Payload";

    /**
     * Assume "v1" if not present.
     */
    private static final String EXTRAS_PROTOCOL_VERSION = "PROTOCOL_VERSION";

    /**
     * version 1: the initial version.
     * version 2: replaced pause/resume trackpoints for track segmentation (lat=100 / lat=200) by TrackPoint.Type.
     */
    private static final int CURRENT_VERSION = 2;

    private static final String EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK = "EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK";
    private static final String EXTRAS_SHOULD_KEEP_SCREEN_ON = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    // WARNING: the value below intentionally duplicates EXTRAS_SHOULD_KEEP_SCREEN_ON. This is a long-standing
    // quirk of the public Intent protocol: deployed dashboards (e.g. OSMDashboard) expect exactly these keys.
    // "Fixing" the string would change the on-the-wire protocol and break compatibility, so it is preserved
    // verbatim. Any correction must be coordinated with a PROTOCOL_VERSION bump. See README_API.md.
    private static final String EXTRAS_SHOW_WHEN_LOCKED = "EXTRAS_SHOULD_KEEP_SCREEN_ON";
    private static final String EXTRAS_SHOW_FULLSCREEN = "EXTRAS_SHOULD_FULLSCREEN";

    private static final int TRACK_URI_INDEX = 0;
    private static final int TRACKPOINTS_URI_INDEX = 1;
    private static final int MARKERS_URI_INDEX = 2;

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

        Intent intent = createDashboardIntent(isRecording, targetPackage, targetClass, trackIds);

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Dashboard not installed; cannot start it.");
            Toast.makeText(context, R.string.show_on_dashboard_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Builds the Dashboard {@link Intent}: the track payload URIs, the protocol extras, and the
     * temporary read-only grant. Optionally targets an explicit component.
     * <p>
     * This method performs no I/O and starts no {@link android.app.Activity}; it is the single place
     * where the Dashboard Intent is assembled, which keeps the privacy boundary and the on-the-wire
     * protocol auditable in one location (and makes them unit-testable). The returned Intent always
     * grants read-only access ({@link Intent#FLAG_GRANT_READ_URI_PERMISSION}) and never write access.
     *
     * @param isRecording   whether the track(s) are currently being recorded
     * @param targetPackage optional explicit target package (combine with {@code targetClass} for an explicit Intent)
     * @param targetClass   optional explicit target class
     * @param trackIds      the user-selected track ids (expected non-empty)
     * @return the fully-assembled Dashboard Intent
     */
    @NonNull
    public static Intent createDashboardIntent(boolean isRecording, @Nullable String targetPackage, @Nullable String targetClass, @NonNull Track.Id... trackIds) {
        ArrayList<Uri> uris = createPayloadUris(trackIds);

        Intent intent = new Intent(ACTION_DASHBOARD);
        intent.putExtra(EXTRAS_PROTOCOL_VERSION, CURRENT_VERSION);

        intent.putParcelableArrayListExtra(ACTION_DASHBOARD_PAYLOAD, uris);

        intent.putExtra(EXTRAS_SHOULD_KEEP_SCREEN_ON, PreferencesUtils.shouldKeepScreenOn());
        intent.putExtra(EXTRAS_SHOW_WHEN_LOCKED, PreferencesUtils.shouldShowStatsOnLockscreen());
        intent.putExtra(EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK, isRecording);
        if (isRecording) {
            intent.putExtra(EXTRAS_SHOW_FULLSCREEN, PreferencesUtils.shouldUseFullscreen());
        }

        grantTemporaryReadAccess(intent, uris);

        if (targetPackage != null && targetClass != null) {
            Log.i(TAG, "Starting dashboard activity with explicit intent (package=" + targetPackage + ", class=" + targetClass + ")");
            intent.setClassName(targetPackage, targetClass);
        } else {
            Log.i(TAG, "Starting dashboard activity with generic intent (package=" + targetPackage + ", class=" + targetClass + ")");
        }

        return intent;
    }

    /**
     * Builds the ordered list of content URIs exposed to the dashboard: track summary, track points
     * and markers, in {@link #TRACK_URI_INDEX}/{@link #TRACKPOINTS_URI_INDEX}/{@link #MARKERS_URI_INDEX} order.
     */
    @NonNull
    private static ArrayList<Uri> createPayloadUris(@NonNull Track.Id... trackIds) {
        String trackIdList = ContentProviderUtils.formatIdListForUri(trackIds);

        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(TRACK_URI_INDEX, Uri.withAppendedPath(TracksColumns.CONTENT_URI, trackIdList));
        uris.add(TRACKPOINTS_URI_INDEX, Uri.withAppendedPath(TrackPointsColumns.CONTENT_URI_BY_TRACKID, trackIdList));
        uris.add(MARKERS_URI_INDEX, Uri.withAppendedPath(MarkerColumns.CONTENT_URI_BY_TRACKID, trackIdList));
        return uris;
    }

    /**
     * Grants the receiver <em>temporary, read-only</em> access to every payload URI.
     * <p>
     * This is the single choke point for the Dashboard privacy boundary: only
     * {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} is ever added and write access is never granted.
     * All URIs are mirrored into the {@link ClipData} so the grant covers the whole payload.
     */
    private static void grantTemporaryReadAccess(@NonNull Intent intent, @NonNull List<Uri> uris) {
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        ClipData clipData = ClipData.newRawUri(null, uris.get(TRACK_URI_INDEX));
        clipData.addItem(new ClipData.Item(uris.get(TRACKPOINTS_URI_INDEX)));
        clipData.addItem(new ClipData.Item(uris.get(MARKERS_URI_INDEX)));
        intent.setClipData(clipData);
    }
}
