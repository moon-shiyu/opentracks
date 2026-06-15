/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.List;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.GpsStatusValue;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.services.announcement.VoiceAnnouncementManager;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;
import de.dennisguse.opentracks.settings.PreferencesUtils;

public class TrackRecordingService extends Service implements TrackPointCreator.Callback, SharedPreferences.OnSharedPreferenceChangeListener, TrackRecordingManager.IdleObserver {

    private static final String TAG = TrackRecordingService.class.getSimpleName();

    private static final Duration RECORDING_DATA_UPDATE_INTERVAL = Duration.ofSeconds(1);

    public static final RecordingStatus STATUS_DEFAULT = RecordingStatus.notRecording();
    public static final RecordingData NOT_RECORDING = new RecordingData(null, null, null);
    public static final GpsStatusValue STATUS_GPS_DEFAULT = GpsStatusValue.GPS_NONE;

    public TrackPoint getLastStoredTrackPointWithLocation() {
        return trackRecordingManager.getLastStoredTrackPointWithLocation();
    }

    public class Binder extends android.os.Binder {

        private Binder() {
            super();
        }

        public TrackRecordingService getService() {
            return TrackRecordingService.this;
        }
    }

    private final Binder binder = new Binder();

    private final Runnable updateRecordingData = new Runnable() {
        @Override
        public void run() {
            updateRecordingDataWhileRecording();

            TrackRecordingService.this.handler.postDelayed(this, RECORDING_DATA_UPDATE_INTERVAL.toMillis());
        }
    };

    // The following variables are set in onCreate:
    private RecordingStatus recordingStatus;
    private MutableLiveData<RecordingStatus> recordingStatusObservable;
    private MutableLiveData<GpsStatusValue> gpsStatusObservable;
    private MutableLiveData<RecordingData> recordingDataObservable;

    // The following variables are set when recording:
    private Handler handler;

    private TrackPointCreator trackPointCreator;
    private TrackRecordingManager trackRecordingManager;

    private VoiceAnnouncementManager voiceAnnouncementManager;
    private TrackRecordingServiceNotificationManager notificationManager;
    private SensorLifecycleManager sensorLifecycleManager;
    private List<SharedPreferences.OnSharedPreferenceChangeListener> preferenceListeners;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Create");

        handler = new Handler(Looper.getMainLooper());

        recordingStatusObservable = new MutableLiveData<>();
        updateRecordingStatus(STATUS_DEFAULT);
        gpsStatusObservable = new MutableLiveData<>(STATUS_GPS_DEFAULT);
        recordingDataObservable = new MutableLiveData<>(NOT_RECORDING);

        trackPointCreator = new TrackPointCreator(this);
        trackRecordingManager = new TrackRecordingManager(this, trackPointCreator, this, handler);

        voiceAnnouncementManager = new VoiceAnnouncementManager(this);
        notificationManager = new TrackRecordingServiceNotificationManager(this);
        sensorLifecycleManager = new SensorLifecycleManager(this, trackPointCreator, notificationManager);

        preferenceListeners = List.of(
                voiceAnnouncementManager, trackRecordingManager,
                trackPointCreator, notificationManager);

        PreferencesUtils.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying");
        if (isRecording()) {
            endCurrentTrack();
        }
        if (isSensorStarted()) {
            stopSensors();
        }

        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(this);

        // Reverse order from onCreate
        preferenceListeners = null;
        sensorLifecycleManager = null;
        notificationManager = null;
        voiceAnnouncementManager = null;
        trackRecordingManager = null;
        trackPointCreator = null;

        handler.removeCallbacksAndMessages(null); //Some tests do not finish the recording completely
        handler = null;

        recordingStatusObservable = null;
        gpsStatusObservable = null;
        recordingDataObservable = null;

        Log.d(TAG, "Destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public Binder onBind(Intent intent) {
        return binder;
    }

    public Track.Id startNewTrack() {
        if (isRecording()) {
            Log.w(TAG, "Ignore startNewTrack. Already recording.");
            return null;
        }
        Log.i(TAG, "startNewTrack");

        // Set recording status
        Track.Id trackId = trackRecordingManager.startNewTrack();
        updateRecordingStatus(RecordingStatus.record(trackId));

        startRecording();
        return trackId;
    }

    public void resumeTrack(Track.Id trackId) {
        if (!trackRecordingManager.resumeExistingTrack(trackId)) {
            Log.w(TAG, "Cannot resume a non-existing track.");
            return;
        }
        Log.i(TAG, "resumeTrack");

        updateRecordingStatus(RecordingStatus.record(trackId));

        startRecording();
    }

    private void startRecording() {
        startPeriodicUiDataUpdate();
        startSensors();
        startVoiceAnnouncements();
    }

    private void startPeriodicUiDataUpdate() {
        handler.postDelayed(updateRecordingData, RECORDING_DATA_UPDATE_INTERVAL.toMillis());
    }

    private void startVoiceAnnouncements() {
        voiceAnnouncementManager.start(trackRecordingManager.getTrackStatistics());
    }

    public void tryStartSensors() {
        if (isSensorStarted()) return;

        Log.i(TAG, "tryStartSensors");

        startSensors();
    }

    private void startSensors() {
        sensorLifecycleManager.start(this, handler);
    }

    public void endCurrentTrack() {
        if (!isRecording()) {
            Log.w(TAG, "Ignore endCurrentTrack. Not recording.");
            return;
        }

        updateRecordingStatus(STATUS_DEFAULT);
        stopRecordingPipeline();
        stopSensors();
    }

    private void stopRecordingPipeline() {
        trackRecordingManager.endCurrentTrack();
        stopUpdateRecordingData();
        voiceAnnouncementManager.stop();
    }

    void stopSensors() {
        if (isRecording()) {
            Log.w(TAG, "Ignore stopSensors. Currently recording.");
            return;
        }
        sensorLifecycleManager.stop();
        gpsStatusObservable.postValue(STATUS_GPS_DEFAULT);
    }

    public Marker.Id createMarker() {
        if (!isRecording()) {
            return null;
        }

        //TODO This contains some duplication to TrackRecodingActivity's Marker creation
        TrackPoint trackPoint = trackRecordingManager.getLastStoredTrackPointWithLocation();
        if (trackPoint == null) {
            return null;
        }
        Marker marker = new Marker(recordingStatus.trackId(), trackPoint);
        return new ContentProviderUtils(this).insertMarker(marker);
    }

    @Override
    public boolean newTrackPoint(TrackPoint trackPoint, Distance thresholdHorizontalAccuracy) {
        if (!isRecording()) {
            Log.w(TAG, "Ignore newTrackPoint. Not recording.");
            return false;
        }

        boolean stored = storeTrackPoint(trackPoint);
        updateNotificationWithTrackPoint(trackRecordingManager.getTrackStatistics(), trackPoint, thresholdHorizontalAccuracy);
        return stored;
    }

    private boolean storeTrackPoint(TrackPoint trackPoint) {
        return trackRecordingManager.onNewTrackPoint(trackPoint);
    }

    private void updateNotificationWithTrackPoint(TrackStatistics trackStatistics, TrackPoint trackPoint, Distance thresholdHorizontalAccuracy) {
        notificationManager.updateTrackPoint(this, trackStatistics, trackPoint, thresholdHorizontalAccuracy);
    }

    @Override
    public void newGpsStatus(GpsStatusValue gpsStatusValue) {
        Log.i(TAG, "newGpsStatus: " + gpsStatusValue.message);

        if (notificationManager == null) {

            StringWriter writer = new StringWriter();
            Exception e = new RuntimeException("TrackRecording.newGpsStatus() called after onDestroy(); objectID: " + this + " with thread: " + Thread.currentThread());
            e.printStackTrace(new PrintWriter(writer));

            Log.e(TAG, e.getMessage() + " " + writer);
            return;
        }
        updateNotificationWithGpsStatus(gpsStatusValue);
        publishGpsStatus(gpsStatusValue);
    }

    private void updateNotificationWithGpsStatus(GpsStatusValue gpsStatusValue) {
        notificationManager.updateContent(getString(gpsStatusValue.message));
    }

    private void publishGpsStatus(GpsStatusValue gpsStatusValue) {
        gpsStatusObservable.postValue(gpsStatusValue);
    }

    @Deprecated
    @VisibleForTesting
    public TrackPointCreator getTrackPointCreator() {
        return trackPointCreator;
    }

    @Deprecated
    @VisibleForTesting
    public TrackRecordingManager getTrackRecordingManager() {
        return trackRecordingManager;
    }

    public LiveData<GpsStatusValue> getGpsStatusObservable() {
        return gpsStatusObservable;
    }

    public LiveData<RecordingData> getRecordingDataObservable() {
        return recordingDataObservable;
    }

    private void updateRecordingDataWhileRecording() {
        if (!isRecording()) {
            Log.w(TAG, "Currently not recording; cannot update data.");
            return;
        }

        // Compute temporary track statistics using sensorData and update time.
        Pair<Track, Pair<TrackPoint, SensorDataSet>> data = trackRecordingManager.getDataForUI();

        voiceAnnouncementManager.announceStatisticsIfNeeded(data.first, data.second.second);

        recordingDataObservable.postValue(new RecordingData(data.first, data.second.first, data.second.second));
    }

    public void onIdle() {
        voiceAnnouncementManager.announceIdle();
    }

    @VisibleForTesting
    public void stopUpdateRecordingData() {
        handler.removeCallbacks(updateRecordingData);
    }

    public LiveData<RecordingStatus> getRecordingStatusObservable() {
        return recordingStatusObservable;
    }

    private void updateRecordingStatus(RecordingStatus status) {
        Log.i(TAG, "new status " + recordingStatus + " -> " + status);
        recordingStatus = status;
        recordingStatusObservable.postValue(recordingStatus);
    }

    @Deprecated //TODO Should be @VisibleForTesting
    public boolean isRecording() {
        return recordingStatus.isRecording();
    }

    private boolean isSensorStarted() {
        return sensorLifecycleManager.isStarted();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        for (SharedPreferences.OnSharedPreferenceChangeListener listener : preferenceListeners) {
            listener.onSharedPreferenceChanged(sharedPreferences, key);
        }
    }
}