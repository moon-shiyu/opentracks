package de.dennisguse.opentracks.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.Service;
import android.os.Handler;
import android.os.PowerManager.WakeLock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import de.dennisguse.opentracks.services.handlers.TrackPointCreator;

/**
 * Tests for {@link SensorLifecycleManager}.
 * <p>
 * Note: This test uses the {@link VisibleForTesting} constructor to inject a mock {@link WakeLock},
 * bypassing {@link de.dennisguse.opentracks.util.SystemUtils} static calls and
 * {@link androidx.core.app.ServiceCompat} static calls.
 * The full integration is tested via {@link TrackRecordingServiceStateMachineTest}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SensorLifecycleManagerTest {

    @Mock
    private Service serviceMock;

    @Mock
    private TrackPointCreator trackPointCreatorMock;

    @Mock
    private TrackRecordingServiceNotificationManager notificationManagerMock;

    @Mock
    private WakeLock wakeLockMock;

    @Mock
    private Handler handlerMock;

    private SensorLifecycleManager createSubject(WakeLock initialWakeLock) {
        return new SensorLifecycleManager(serviceMock, trackPointCreatorMock, notificationManagerMock, initialWakeLock);
    }

    @Test
    public void isStarted_returnsFalseInitially() {
        SensorLifecycleManager subject = createSubject(null);
        assertFalse(subject.isStarted());
    }

    @Test
    public void isStarted_returnsTrueWhenWakeLockPresent() {
        SensorLifecycleManager subject = createSubject(wakeLockMock);
        assertTrue(subject.isStarted());
    }

    @Test
    public void stop_callsTrackPointCreatorStop() {
        SensorLifecycleManager subject = createSubject(wakeLockMock);

        subject.stop();

        verify(trackPointCreatorMock).stop();
    }

    @Test
    public void stop_callsStopForeground() {
        SensorLifecycleManager subject = createSubject(wakeLockMock);

        subject.stop();

        verify(serviceMock).stopForeground(true);
    }

    @Test
    public void stop_cancelsNotification() {
        SensorLifecycleManager subject = createSubject(wakeLockMock);

        subject.stop();

        verify(notificationManagerMock).cancelNotification();
    }

    @Test
    public void start_whenAlreadyStarted_isNoop() {
        SensorLifecycleManager subject = createSubject(wakeLockMock);
        android.content.Context contextMock = mock(android.content.Context.class);

        subject.start(contextMock, handlerMock);

        // Should not call trackPointCreator.start again since already started
        verify(trackPointCreatorMock, never()).start(contextMock, handlerMock);
    }
}
