package de.dennisguse.opentracks.sensors.sensorData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.BluetoothHandlerCyclingDistanceSpeed;
import de.dennisguse.opentracks.sensors.BluetoothHandlerManagerCyclingPower;
import de.dennisguse.opentracks.sensors.GpsStatusValue;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;

/**
 * Acceptance scenario: a workout recorded with BLE sensors only (no GNSS fix) must still fuse the sensor
 * aggregators into a populated {@link TrackPoint} that carries no location.
 */
@RunWith(AndroidJUnit4.class)
public class SensorDataSetTest {

    private static final String TIME = "2020-01-01T00:00:00Z";

    @Test
    public void fillTrackPoint_bluetoothOnly_noGps() {
        TrackPointCreator trackPointCreator = new TrackPointCreator(new TrackPointCreator.Callback() {
            @Override
            public boolean newTrackPoint(TrackPoint trackPoint, Distance thresholdHorizontalAccuracy) {
                return false;
            }

            @Override
            public void newGpsStatus(GpsStatusValue gpsStatusValue) {
            }
        });
        trackPointCreator.setClock(TIME);

        SensorDataSet sensorDataSet = new SensorDataSet(trackPointCreator);

        AggregatorHeartRate heartRate = new AggregatorHeartRate("hr", "hr");
        heartRate.add(new Raw<>(Instant.parse(TIME), HeartRate.of(90)));
        sensorDataSet.add(heartRate);

        AggregatorCyclingPower power = new AggregatorCyclingPower("pw", "pw");
        power.add(new Raw<>(Instant.parse(TIME), new BluetoothHandlerManagerCyclingPower.Data(Power.of(123f), null)));
        sensorDataSet.add(power);

        // Speed/distance requires two wheel samples: +1 revolution of a 2 m wheel over 1 s => 2 m at 2 m/s.
        AggregatorCyclingDistanceSpeed distanceSpeed = new AggregatorCyclingDistanceSpeed("cs", "cs");
        distanceSpeed.setWheelCircumference(Distance.ofMM(2000));
        distanceSpeed.add(new Raw<>(Instant.parse(TIME), new BluetoothHandlerCyclingDistanceSpeed.WheelData(0, 0)));
        distanceSpeed.add(new Raw<>(Instant.parse(TIME), new BluetoothHandlerCyclingDistanceSpeed.WheelData(1, 1024)));
        sensorDataSet.add(distanceSpeed);

        // when
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, trackPointCreator.createNow());
        sensorDataSet.fillTrackPoint(trackPoint);

        // then
        assertFalse(trackPoint.hasLocation());

        assertEquals(HeartRate.of(90), trackPoint.getHeartRate());

        assertNotNull(trackPoint.getPower());
        assertEquals(123f, trackPoint.getPower().getW(), 0.01);

        assertNotNull(trackPoint.getSpeed());
        assertEquals(2.0, trackPoint.getSpeed().toMPS(), 0.01);

        assertNotNull(trackPoint.getSensorDistance());
        assertEquals(2.0, trackPoint.getSensorDistance().toM(), 0.01);
    }
}
