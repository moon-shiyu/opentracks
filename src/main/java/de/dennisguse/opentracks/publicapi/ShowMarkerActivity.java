package de.dennisguse.opentracks.publicapi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.databinding.MarkerDetailActivityBinding;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.ui.markers.MarkerDetailActivity;
import de.dennisguse.opentracks.util.IntentUtils;

/**
 * INTERNAL: only meant for clients of OSMDashboard API.
 */
public class ShowMarkerActivity extends AppCompatActivity {

    public static final String EXTRA_MARKER_ID = "marker_id";

    private static final String TAG = ShowMarkerActivity.class.getSimpleName();

    private MarkerDetailActivityBinding viewBinding;

    private List<Marker.Id> markerIds;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        if (!PreferencesUtils.isPublicAPIenabled()) {
            Toast.makeText(this, getString(R.string.settings_public_api_disabled_toast), Toast.LENGTH_LONG).show();
            Log.w(TAG, "Public API is disabled; ignoring ShowMarker request.");
            finish();
            return;
        }

        if (!getIntent().hasExtra(EXTRA_MARKER_ID)) {
            throw new IllegalStateException("Parameter 'markerId' missing");
        }

        Marker.Id markerId = new Marker.Id(getIntent().getLongExtra(EXTRA_MARKER_ID, -1));
        Intent intent = IntentUtils.newIntent(this, MarkerDetailActivity.class)
                .putExtra(MarkerDetailActivity.EXTRA_MARKER_ID, markerId);
        startActivity(intent);
        finish();
    }

}