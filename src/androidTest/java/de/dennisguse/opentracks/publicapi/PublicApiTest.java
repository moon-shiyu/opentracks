package de.dennisguse.opentracks.publicapi;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.IntentDashboardUtils;
import de.dennisguse.opentracks.util.IntentUtils;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class PublicApiTest {

    @Rule
    public GrantPermissionRule mGrantPermissionRule = TestUtil.createGrantPermissionRule();

    private final Context context = ApplicationProvider.getApplicationContext();

    //NOTE: this doesn't check if the TrackRecordingService was started in foreground.
    @Test
    public void StartStopTest() throws InterruptedException {
        PreferencesUtils.setBoolean(R.string.publicapi_enabled_key, true);
        Intent startIntent = IntentUtils.newIntent(context, StartRecording.class);
        startIntent.putExtra("TRACK_NAME", "trackName");
        startIntent.putExtra("TRACK_CATEGORY", "activityTypeLocalized");
        startIntent.putExtra("TRACK_ICON", "airplane");
        startIntent.putExtra("TRACK_DESCRIPTION", "description");
        context.startActivity(startIntent);

        Thread.sleep(5000);

        context.startActivity(IntentUtils.newIntent(context, StopRecording.class));

        List<Track> tracks = new ContentProviderUtils(context).getTracks();
        Assert.assertEquals(1, tracks.size());
        Track track = tracks.get(0);
        Assert.assertEquals("trackName", track.getName());
        Assert.assertEquals("activityTypeLocalized", track.getActivityTypeLocalized());
        Assert.assertEquals("airplane", track.getActivityType().getId());
    }

    @Test
    public void StopAndWait() throws InterruptedException {
        PreferencesUtils.setBoolean(R.string.publicapi_enabled_key, true);

        context.startActivity(IntentUtils.newIntent(context, StopRecording.class));

        Thread.sleep(10000);

        //No ForegroundServiceDidNotStartInTimeException should be happening.
    }

    // ---------------------------------------------------------------------------------------------
    // IntentDashboardUtils: privacy boundary + on-the-wire protocol.
    //
    // These verify the Dashboard Intent assembled by IntentDashboardUtils#createDashboardIntent
    // WITHOUT launching any Activity (the method does no I/O). They guard:
    //   - the privacy contract: a temporary READ-only grant, never write/persistable/prefix;
    //   - the protocol relied upon by third-party dashboards (e.g. OSMDashboard): action, payload,
    //     protocol version and recording flag.
    // The string keys/values below are deliberately hard-coded so the test fails if the public
    // protocol is changed accidentally.
    // ---------------------------------------------------------------------------------------------

    private static final String DASHBOARD_ACTION = "Intent.OpenTracks-Dashboard";
    private static final String DASHBOARD_PAYLOAD = DASHBOARD_ACTION + ".Payload";
    private static final String DASHBOARD_PROTOCOL_VERSION = "PROTOCOL_VERSION";
    private static final String DASHBOARD_IS_RECORDING = "EXTRAS_OPENTRACKS_IS_RECORDING_THIS_TRACK";

    @Test
    public void dashboardIntent_grantsTemporaryReadAccess_neverWrite() {
        Intent intent = IntentDashboardUtils.createDashboardIntent(true, null, null, new Track.Id(1L));

        int flags = intent.getFlags();
        Assert.assertTrue("Dashboard Intent must grant temporary read access",
                (flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        Assert.assertEquals("Dashboard Intent must NEVER grant write access",
                0, (flags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        Assert.assertEquals("Dashboard Intent must NEVER grant persistable access",
                0, (flags & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
        Assert.assertEquals("Dashboard Intent must NEVER grant prefix access",
                0, (flags & Intent.FLAG_GRANT_PREFIX_URI_PERMISSION));
    }

    @Test
    public void dashboardIntent_exposesTrackTrackpointsMarkers_asContentUris() {
        Intent intent = IntentDashboardUtils.createDashboardIntent(false, null, null, new Track.Id(1L), new Track.Id(2L));

        Assert.assertEquals(DASHBOARD_ACTION, intent.getAction());
        Assert.assertTrue("payload extra must be present", intent.hasExtra(DASHBOARD_PAYLOAD));

        ClipData clipData = intent.getClipData();
        Assert.assertNotNull("ClipData must carry the granted URIs so the read grant covers them", clipData);
        Assert.assertEquals("Track + TrackPoints + Markers = 3 URIs", 3, clipData.getItemCount());
        for (int i = 0; i < clipData.getItemCount(); i++) {
            Uri uri = clipData.getItemAt(i).getUri();
            Assert.assertNotNull(uri);
            Assert.assertEquals("only content:// URIs may be exposed", "content", uri.getScheme());
        }
    }

    @Test
    public void dashboardIntent_carriesCurrentProtocolAndRecordingState() {
        Intent recording = IntentDashboardUtils.createDashboardIntent(true, null, null, new Track.Id(1L));
        Assert.assertEquals("protocol version must stay 2 for compatibility",
                2, recording.getIntExtra(DASHBOARD_PROTOCOL_VERSION, -1));
        Assert.assertTrue("recording flag must propagate",
                recording.getBooleanExtra(DASHBOARD_IS_RECORDING, false));

        Intent notRecording = IntentDashboardUtils.createDashboardIntent(false, null, null, new Track.Id(1L));
        Assert.assertFalse("non-recording flag must propagate",
                notRecording.getBooleanExtra(DASHBOARD_IS_RECORDING, true));
    }

    @Test
    public void dashboardIntent_explicitTarget_setsComponent_genericLeavesItUnset() {
        Intent explicit = IntentDashboardUtils.createDashboardIntent(
                true, "com.example.dashboard", "com.example.dashboard.MainActivity", new Track.Id(1L));
        Assert.assertNotNull("an explicit target must pin a component", explicit.getComponent());
        Assert.assertEquals("com.example.dashboard", explicit.getComponent().getPackageName());
        Assert.assertEquals("com.example.dashboard.MainActivity", explicit.getComponent().getClassName());
        // An explicit Intent must remain read-only too.
        Assert.assertTrue((explicit.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
        Assert.assertEquals(0, (explicit.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION));

        Intent generic = IntentDashboardUtils.createDashboardIntent(true, null, null, new Track.Id(1L));
        Assert.assertNull("a generic intent must not pin a component", generic.getComponent());
    }
}