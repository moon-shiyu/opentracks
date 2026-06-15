package de.dennisguse.opentracks.publicapi;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.util.IntentDashboardUtils;

/**
 * Parses dashboard target extras from a Bundle and triggers the dashboard Intent.
 * <p>
 * Reads {@link PublicApiConstants#EXTRA_STATS_TARGET_PACKAGE} and
 * {@link PublicApiConstants#EXTRA_STATS_TARGET_CLASS} from the Bundle.
 * If both are present, starts the dashboard with an explicit Intent.
 */
final class DashboardTriggerHelper {

    private DashboardTriggerHelper() {
    }

    /**
     * If the bundle contains both {@link PublicApiConstants#EXTRA_STATS_TARGET_PACKAGE}
     * and {@link PublicApiConstants#EXTRA_STATS_TARGET_CLASS}, starts the dashboard
     * with an explicit Intent targeting that component.
     * Otherwise does nothing (the user can still trigger the dashboard from the UI).
     *
     * @param context the calling context
     * @param trackId the track to display
     * @param bundle  the Intent extras
     */
    static void startDashboardIfConfigured(@NonNull Context context, @NonNull Track.Id trackId, @NonNull Bundle bundle) {
        String targetPackage = bundle.getString(PublicApiConstants.EXTRA_STATS_TARGET_PACKAGE, null);
        String targetClass = bundle.getString(PublicApiConstants.EXTRA_STATS_TARGET_CLASS, null);
        if (targetClass != null && targetPackage != null) {
            IntentDashboardUtils.startDashboard(context, true, targetPackage, targetClass, trackId);
        }
    }
}
