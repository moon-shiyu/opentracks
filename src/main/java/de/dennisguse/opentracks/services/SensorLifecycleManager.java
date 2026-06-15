package de.dennisguse.opentracks.services;

import android.app.Service;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.ServiceCompat;

import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.util.SystemUtils;

/**
 * Manages the lifecycle of sensors during recording, encapsulating three tightly-coupled concerns
 * that always start and stop together: WakeLock acquisition, sensor pipeline (via {@link TrackPointCreator}),
 * and foreground service notification.
 * <p>
 * This class owns the {@link WakeLock} and coordinates with {@link TrackRecordingServiceNotificationManager}
 * to promote/demote the service to/from foreground state.
 */
class SensorLifecycleManager {

    private static final String TAG = SensorLifecycleManager.class.getSimpleName();

    private final Service service;
    private final TrackPointCreator trackPointCreator;
    private final TrackRecordingServiceNotificationManager notificationManager;

    private WakeLock wakeLock;

    SensorLifecycleManager(Service service, TrackPointCreator trackPointCreator, TrackRecordingServiceNotificationManager notificationManager) {
        this.service = service;
        this.trackPointCreator = trackPointCreator;
        this.notificationManager = notificationManager;
    }

    @VisibleForTesting
    SensorLifecycleManager(Service service, TrackPointCreator trackPointCreator, TrackRecordingServiceNotificationManager notificationManager, WakeLock initialWakeLock) {
        this.service = service;
        this.trackPointCreator = trackPointCreator;
        this.notificationManager = notificationManager;
        this.wakeLock = initialWakeLock;
    }

    synchronized void start(Context context, Handler handler) {
        if (isStarted()) {
            Log.i(TAG, "sensors already started; skipping");
            return;
        }
        Log.i(TAG, "start");
        acquireWakeLock(context);
        trackPointCreator.start(context, handler);
        startForegroundWithNotification(context);
    }

    void stop() {
        trackPointCreator.stop();
        stopForegroundWithNotification();
        releaseWakeLock();
    }

    boolean isStarted() {
        return wakeLock != null;
    }

    private void acquireWakeLock(Context context) {
        wakeLock = SystemUtils.acquireWakeLock(context, wakeLock);
    }

    private void releaseWakeLock() {
        wakeLock = SystemUtils.releaseWakeLock(wakeLock);
    }

    private void startForegroundWithNotification(Context context) {
        ServiceCompat.startForeground(service,
                TrackRecordingServiceNotificationManager.NOTIFICATION_ID,
                notificationManager.setGPSonlyStarted(context),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION + ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
    }

    private void stopForegroundWithNotification() {
        service.stopForeground(true);
        notificationManager.cancelNotification();
    }
}
