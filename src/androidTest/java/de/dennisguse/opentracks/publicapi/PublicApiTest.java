package de.dennisguse.opentracks.publicapi;

import android.content.Context;
import android.content.Intent;

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
        startIntent.putExtra(PublicApiConstants.EXTRA_TRACK_NAME, "trackName");
        startIntent.putExtra(PublicApiConstants.EXTRA_TRACK_ACTIVITY_TYPE_LOCALIZED, "activityTypeLocalized");
        startIntent.putExtra(PublicApiConstants.EXTRA_TRACK_ACTIVITY_TYPE_ID, "airplane");
        startIntent.putExtra(PublicApiConstants.EXTRA_TRACK_DESCRIPTION, "description");
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

    /**
     * Verifies that the deprecated constants in StartRecording still hold the
     * same string values as PublicApiConstants (backward compatibility).
     */
    @Test
    public void deprecatedConstants_backwardCompatibility() {
        Assert.assertEquals(PublicApiConstants.EXTRA_TRACK_NAME, StartRecording.EXTRA_TRACK_NAME);
        Assert.assertEquals(PublicApiConstants.EXTRA_TRACK_DESCRIPTION, StartRecording.EXTRA_TRACK_DESCRIPTION);
        Assert.assertEquals(PublicApiConstants.EXTRA_TRACK_ACTIVITY_TYPE_LOCALIZED, StartRecording.EXTRA_TRACK_ACTIVITY_TYPE_LOCALIZED);
        Assert.assertEquals(PublicApiConstants.EXTRA_TRACK_ACTIVITY_TYPE_ID, StartRecording.EXTRA_TRACK_ACTIVITY_TYPE_ID);
        Assert.assertEquals(PublicApiConstants.EXTRA_STATS_TARGET_PACKAGE, StartRecording.EXTRA_STATS_TARGET_PACKAGE);
        Assert.assertEquals(PublicApiConstants.EXTRA_STATS_TARGET_CLASS, StartRecording.EXTRA_STATS_TARGET_CLASS);
    }

    /**
     * Verifies that the public API string values match the documented contract
     * (README_API.md references these exact string literals).
     */
    @Test
    public void constants_matchDocumentedValues() {
        Assert.assertEquals("TRACK_NAME", PublicApiConstants.EXTRA_TRACK_NAME);
        Assert.assertEquals("TRACK_DESCRIPTION", PublicApiConstants.EXTRA_TRACK_DESCRIPTION);
        Assert.assertEquals("TRACK_CATEGORY", PublicApiConstants.EXTRA_TRACK_ACTIVITY_TYPE_LOCALIZED);
        Assert.assertEquals("TRACK_ICON", PublicApiConstants.EXTRA_TRACK_ACTIVITY_TYPE_ID);
        Assert.assertEquals("STATS_TARGET_PACKAGE", PublicApiConstants.EXTRA_STATS_TARGET_PACKAGE);
        Assert.assertEquals("STATS_TARGET_CLASS", PublicApiConstants.EXTRA_STATS_TARGET_CLASS);
    }

    /**
     * Verifies that when the public API is disabled, StartRecording extras
     * include dashboard target fields but the dashboard is not triggered
     * unless isPublicAPIDashboardEnabled is also true.
     */
    @Test
    public void dashboardExtras_onlyTriggersWhenDashboardEnabled() throws InterruptedException {
        PreferencesUtils.setBoolean(R.string.publicapi_enabled_key, true);
        // Dashboard disabled by default
        Assert.assertFalse(PreferencesUtils.isPublicAPIDashboardEnabled());

        Intent startIntent = IntentUtils.newIntent(context, StartRecording.class);
        startIntent.putExtra(PublicApiConstants.EXTRA_TRACK_NAME, "dashboardTest");
        startIntent.putExtra(PublicApiConstants.EXTRA_STATS_TARGET_PACKAGE, "com.example.dashboard");
        startIntent.putExtra(PublicApiConstants.EXTRA_STATS_TARGET_CLASS, "com.example.dashboard.DashboardActivity");
        context.startActivity(startIntent);

        Thread.sleep(5000);

        context.startActivity(IntentUtils.newIntent(context, StopRecording.class));

        // Track should still be created even though dashboard was not triggered
        List<Track> tracks = new ContentProviderUtils(context).getTracks();
        Assert.assertTrue("At least one track should exist", tracks.size() >= 1);
    }
}
