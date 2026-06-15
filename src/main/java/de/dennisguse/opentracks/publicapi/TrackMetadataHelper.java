package de.dennisguse.opentracks.publicapi;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Track;

/**
 * Applies track metadata from public API Intent extras to an existing Track.
 * <p>
 * Reads {@link PublicApiConstants#EXTRA_TRACK_NAME},
 * {@link PublicApiConstants#EXTRA_TRACK_DESCRIPTION},
 * {@link PublicApiConstants#EXTRA_TRACK_ACTIVITY_TYPE_ID}, and
 * {@link PublicApiConstants#EXTRA_TRACK_ACTIVITY_TYPE_LOCALIZED} from the Bundle.
 */
final class TrackMetadataHelper {

    private TrackMetadataHelper() {
    }

    /**
     * Reads track metadata extras from the bundle and updates the track via ContentProvider.
     * Extras not present in the bundle are left at their defaults (empty string / null activity type).
     *
     * @param context used to access ContentProviderUtils
     * @param trackId the track to update
     * @param bundle  the Intent extras containing track metadata
     */
    static void updateTrackMetadata(@NonNull Context context, @NonNull Track.Id trackId, @NonNull Bundle bundle) {
        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);
        Track track = contentProviderUtils.getTrack(trackId);

        track.setName(bundle.getString(PublicApiConstants.EXTRA_TRACK_NAME, ""));
        track.setDescription(bundle.getString(PublicApiConstants.EXTRA_TRACK_DESCRIPTION, ""));
        track.setActivityType(ActivityType.findBy(bundle.getString(PublicApiConstants.EXTRA_TRACK_ACTIVITY_TYPE_ID, null)));
        track.setActivityTypeLocalized(bundle.getString(PublicApiConstants.EXTRA_TRACK_ACTIVITY_TYPE_LOCALIZED, ""));

        contentProviderUtils.updateTrack(track);
    }
}
