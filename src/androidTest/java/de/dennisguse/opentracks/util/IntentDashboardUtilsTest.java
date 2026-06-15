package de.dennisguse.opentracks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import de.dennisguse.opentracks.data.models.Track;

/**
 * Tests for {@link IntentDashboardUtils}, focusing on:
 * <ul>
 *     <li>Dashboard Intent structure (URIs, ClipData, flags)</li>
 *     <li>Temporary read-only permission model</li>
 *     <li>Explicit vs implicit intent targeting</li>
 *     <li>Recording state extras</li>
 *     <li>Regression: screen-on and lockscreen extras have distinct keys</li>
 * </ul>
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class IntentDashboardUtilsTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void buildDashboardIntent_singleTrack_correctUris() {
        Track.Id trackId = new Track.Id(42);

        Intent intent = IntentDashboardUtils.buildDashboardIntent(
                context, false, null, null, trackId);

        assertNotNull(intent);
        ClipData clipData = intent.getClipData();
        assertNotNull("ClipData must not be null", clipData);
        assertEquals("ClipData must have 3 items (tracks, trackpoints, markers)", 3, clipData.getItemCount());

        // Verify each URI contains the track ID
        for (int i = 0; i < clipData.getItemCount(); i++) {
            Uri uri = clipData.getItemAt(i).getUri();
            assertNotNull("URI at index " + i + " must not be null", uri);
            assertTrue("URI at index " + i + " must contain track ID 42, got: " + uri,
                    uri.toString().contains("42"));
        }
    }

    @Test
    public void buildDashboardIntent_multipleTracks_uriContainsAllIds() {
        Track.Id id1 = new Track.Id(10);
        Track.Id id2 = new Track.Id(20);
        Track.Id id3 = new Track.Id(30);

        Intent intent = IntentDashboardUtils.buildDashboardIntent(
                context, false, null, null, id1, id2, id3);

        ClipData clipData = intent.getClipData();
        assertNotNull(clipData);
        // Verify the first URI (tracks) contains all IDs as comma-separated
        Uri tracksUri = clipData.getItemAt(0).getUri();
        String path = tracksUri.toString();
        assertTrue("URI must contain track ID 10: " + path, path.contains("10"));
        assertTrue("URI must contain track ID 20: " + path, path.contains("20"));
        assertTrue("URI must contain track ID 30: " + path, path.contains("30"));
    }

    @Test
    public void buildDashboardIntent_hasReadPermissionFlag() {
        Intent intent = IntentDashboardUtils.buildDashboardIntent(
                context, false, null, null, new Track.Id(1));

        int flags = intent.getFlags();
        assertTrue("Intent must have FLAG_GRANT_READ_URI_PERMISSION",
                (flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        assertFalse("Intent must NOT have FLAG_GRANT_WRITE_URI_PERMISSION",
                (flags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0);
        assertFalse("Intent must NOT have FLAG_GRANT_PERSISTABLE_URI_PERMISSION",
                (flags & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0);
    }

    @Test
    public void buildDashboardIntent_clipData_hasThreeItems() {
        Intent intent = IntentDashboardUtils.buildDashboardIntent(
                context, false, null, null, new Track.Id(1));

        ClipData clipData = intent.getClipData();
        assertNotNull(clipData);
        assertEquals(3, clipData.getItemCount());

        // Verify each item has a non-null URI
        for (int i = 0; i < 3; i++) {
            assertNotNull("ClipData item " + i + " must have a URI", clipData.getItemAt(i).getUri());
        }
    }

    @Test
    public void buildDashboardIntent_protocolVersion() {
        Intent intent = IntentDashboardUtils.buildDashboardIntent(
                context, false, null, null, new Track.Id(1));

        int version = intent.getIntExtra(IntentDashboardUtils.EXTRAS_PROTOCOL_VERSION, -1);
        assertEquals("Protocol version must be 2", IntentDashboardUtils.CURRENT_VERSION, version);
    }

    @Test
    public void buildDashboardIntent_recordingExtras_recording() {
        Intent intent = IntentDashboardUtils.buildDashboardIntent(
                context, true, null, null, new Track.Id(1));

        assertTrue("isRecording extra must be true",
                intent.getBooleanExtra(IntentDashboardUtils.EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK, false));
        assertTrue("Fullscreen extra must be present when recording",
                intent.hasExtra(IntentDashboardUtils.EXTRAS_SHOW_FULLSCREEN));
    }

    @Test
    public void buildDashboardIntent_recordingExtras_notRecording() {
        Intent intent = IntentDashboardUtils.buildDashboardIntent(
                context, false, null, null, new Track.Id(1));

        assertFalse("isRecording extra must be false",
                intent.getBooleanExtra(IntentDashboardUtils.EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK, true));
        assertFalse("Fullscreen extra must NOT be present when not recording",
                intent.hasExtra(IntentDashboardUtils.EXTRAS_SHOW_FULLSCREEN));
    }

    /**
     * Regression test: EXTRAS_SHOULD_KEEP_SCREEN_ON and EXTRAS_SHOW_WHEN_LOCKED
     * must have distinct keys so both values can coexist in the Intent.
     */
    @Test
    public void buildDashboardIntent_screenOnAndLockscreen_distinctKeys() {
        Intent intent = IntentDashboardUtils.buildDashboardIntent(
                context, false, null, null, new Track.Id(1));

        // Both extras must be present with their correct keys
        assertTrue("Intent must contain EXTRAS_SHOULD_KEEP_SCREEN_ON",
                intent.hasExtra(IntentDashboardUtils.EXTRAS_SHOULD_KEEP_SCREEN_ON));
        assertTrue("Intent must contain EXTRAS_SHOW_WHEN_LOCKED",
                intent.hasExtra(IntentDashboardUtils.EXTRAS_SHOW_WHEN_LOCKED));

        // The keys must be distinct
        assertFalse("Screen-on and lockscreen extras must have different keys",
                IntentDashboardUtils.EXTRAS_SHOULD_KEEP_SCREEN_ON.equals(
                        IntentDashboardUtils.EXTRAS_SHOW_WHEN_LOCKED));
    }

    @Test
    public void buildDashboardIntent_explicitTarget_setsClassName() {
        String pkg = "com.example.dashboard";
        String cls = "com.example.dashboard.DashboardActivity";

        Intent intent = IntentDashboardUtils.buildDashboardIntent(
                context, false, pkg, cls, new Track.Id(1));

        ComponentName component = intent.getComponent();
        assertNotNull("Intent must have an explicit component", component);
        assertEquals(pkg, component.getPackageName());
        assertEquals(cls, component.getClassName());
    }

    @Test
    public void buildDashboardIntent_noTarget_isImplicit() {
        Intent intent = IntentDashboardUtils.buildDashboardIntent(
                context, false, null, null, new Track.Id(1));

        assertNull("Intent must NOT have an explicit component for implicit targeting",
                intent.getComponent());
        assertEquals("Intent action must be the dashboard action",
                IntentDashboardUtils.ACTION_DASHBOARD, intent.getAction());
    }

    @Test
    public void buildDashboardIntent_payloadUrisInExtras() {
        Intent intent = IntentDashboardUtils.buildDashboardIntent(
                context, false, null, null, new Track.Id(99));

        ArrayList<Uri> payloadUris = intent.getParcelableArrayListExtra(
                IntentDashboardUtils.ACTION_DASHBOARD + ".Payload");
        assertNotNull("Payload URI list must not be null", payloadUris);
        assertEquals("Payload must contain 3 URIs", 3, payloadUris.size());
    }

    @Test
    public void startDashboard_emptyTrackIds_noException() {
        // Should return early without throwing
        IntentDashboardUtils.startDashboard(context, false, null, null);
    }
}
