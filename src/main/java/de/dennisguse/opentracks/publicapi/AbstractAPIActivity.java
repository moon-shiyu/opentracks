package de.dennisguse.opentracks.publicapi;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.settings.PreferencesUtils;

abstract class AbstractAPIActivity extends AppCompatActivity {

    private final String TAG = AbstractAPIActivity.class.getSimpleName();

    private final TrackRecordingServiceConnection.Callback serviceConnectedCallback = (service, connection) -> {
        if (!isFinishing() && !isDestroyed()) {
            execute(service);
        }
        releaseService(connection);
        finish();
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        splashScreen.setKeepOnScreenCondition(() -> true);

        // Privacy gate: the public API is opt-in. While disabled, external requests are ignored and
        // no recording service is bound/started and no data is touched.
        if (PreferencesUtils.isPublicAPIenabled()) {
            Log.i(TAG, "Received and trying to execute requested action.");
            if (requiresForeground()) {
                TrackRecordingServiceConnection.executeForeground(this, serviceConnectedCallback);
            } else {
                TrackRecordingServiceConnection.execute(this, serviceConnectedCallback);
            }
        } else {
            Toast.makeText(this, getString(R.string.settings_public_api_disabled_toast), Toast.LENGTH_LONG).show();
            Log.w(TAG, "Public API is disabled; ignoring request.");
            finish();
        }
    }

    protected abstract void execute(TrackRecordingService service);

    protected abstract boolean isPostExecuteStopService();

    protected boolean requiresForeground() {
        return false;
    }

    /**
     * Releases the recording service once {@link #execute(TrackRecordingService)} has run.
     * One-shot calls (e.g. CreateMarker) only unbind and leave the service running; calls that stop a
     * recording ({@link #isPostExecuteStopService()} == true) additionally stop the service.
     */
    private void releaseService(TrackRecordingServiceConnection connection) {
        if (isPostExecuteStopService()) {
            connection.unbindAndStop(this);
        } else {
            connection.unbind(this);
        }
    }
}
