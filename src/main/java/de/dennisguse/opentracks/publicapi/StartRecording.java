package de.dennisguse.opentracks.publicapi;

import android.os.Bundle;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.settings.PreferencesUtils;

public class StartRecording extends AbstractAPIActivity {

    /** @deprecated Use {@link PublicApiConstants#EXTRA_TRACK_NAME} instead. */
    @Deprecated
    public static final String EXTRA_TRACK_NAME = PublicApiConstants.EXTRA_TRACK_NAME;

    /** @deprecated Use {@link PublicApiConstants#EXTRA_TRACK_ACTIVITY_TYPE_LOCALIZED} instead. */
    @Deprecated
    public static final String EXTRA_TRACK_ACTIVITY_TYPE_LOCALIZED = PublicApiConstants.EXTRA_TRACK_ACTIVITY_TYPE_LOCALIZED;

    /** @deprecated Use {@link PublicApiConstants#EXTRA_TRACK_ACTIVITY_TYPE_ID} instead. */
    @Deprecated
    public static final String EXTRA_TRACK_ACTIVITY_TYPE_ID = PublicApiConstants.EXTRA_TRACK_ACTIVITY_TYPE_ID;

    /** @deprecated Use {@link PublicApiConstants#EXTRA_TRACK_DESCRIPTION} instead. */
    @Deprecated
    public static final String EXTRA_TRACK_DESCRIPTION = PublicApiConstants.EXTRA_TRACK_DESCRIPTION;

    /** @deprecated Use {@link PublicApiConstants#EXTRA_STATS_TARGET_PACKAGE} instead. */
    @Deprecated
    public static final String EXTRA_STATS_TARGET_PACKAGE = PublicApiConstants.EXTRA_STATS_TARGET_PACKAGE;

    /** @deprecated Use {@link PublicApiConstants#EXTRA_STATS_TARGET_CLASS} instead. */
    @Deprecated
    public static final String EXTRA_STATS_TARGET_CLASS = PublicApiConstants.EXTRA_STATS_TARGET_CLASS;

    @Override
    protected void execute(TrackRecordingService service) {
        Track.Id trackId = service.startNewTrack();
        if (trackId != null) {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                TrackMetadataHelper.updateTrackMetadata(this, trackId, bundle);

                if (PreferencesUtils.isPublicAPIDashboardEnabled()) {
                    DashboardTriggerHelper.startDashboardIfConfigured(this, trackId, bundle);
                }
            }
        }
    }

    @Override
    protected boolean isPostExecuteStopService() {
        return false;
    }

    @Override
    protected boolean requiresForeground() {
        return true;
    }
}
