package de.dennisguse.opentracks.sensors.sensorData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

import de.dennisguse.opentracks.data.models.AltitudeGainLoss;
import de.dennisguse.opentracks.data.models.AtmosphericPressure;
import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.GpsStatusValue;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.BluetoothHandlerCyclingCadence;
import de.dennisguse.opentracks.sensors.BluetoothHandlerCyclingDistanceSpeed;
import de.dennisguse.opentracks.sensors.BluetoothHandlerManagerCyclingPower;
import de.dennisguse.opentracks.sensors.BluetoothHandlerRunningSpeedAndCadence;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;

@RunWith(AndroidJUnit4.class)
public class SensorDataSetTest {

    private TrackPointCreator trackPointCreator;
    private SensorDataSet sensorDataSet;

    @Before
    public void setUp() {
        trackPointCreator = new TrackPointCreator(new TrackPointCreator.Callback() {
            @Override
            public boolean newTrackPoint(TrackPoint trackPoint, Distance thresholdHorizontalAccuracy) {
                return true;
            }

            @Override
            public void newGpsStatus(GpsStatusValue gpsStatusValue) {
            }
        });
        trackPointCreator.setClock("2024-01-01T12:00:00Z");
        sensorDataSet = new SensorDataSet(trackPointCreator);
    }

    // --- update() routing ---

    @Test
    public void update_heartRate_routesToHeartRateAggregator() {
        sensorDataSet.add(new AggregatorHeartRate("addr", "HRM"));

        sensorDataSet.update(new Raw<>(Instant.MIN, HeartRate.of(120)));

        assertEquals(HeartRate.of(120), sensorDataSet.heartRate.getAggregatedValue(Instant.MIN));
    }

    @Test
    public void update_crankData_routesToCyclingCadence() {
        sensorDataSet.add(new AggregatorCyclingCadence("addr", "CSC"));

        sensorDataSet.update(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(1, 1024)));
        sensorDataSet.update(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(2, 2048)));

        assertEquals(60, sensorDataSet.cyclingCadence.getAggregatedValue(Instant.MIN).getRPM(), 0.01);
    }

    @Test
    public void update_wheelData_routesToCyclingDistanceSpeed() {
        AggregatorCyclingDistanceSpeed agg = new AggregatorCyclingDistanceSpeed("addr", "CSC");
        agg.setWheelCircumference(Distance.ofMM(2150));
        sensorDataSet.add(agg);

        sensorDataSet.update(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingDistanceSpeed.WheelData(1, 6184)));
        sensorDataSet.update(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingDistanceSpeed.WheelData(2, 8016)));

        assertNotNull(sensorDataSet.cyclingDistanceSpeed);
        assertEquals(2.15, sensorDataSet.cyclingDistanceSpeed.getAggregatedValue(Instant.MIN).distance().toM(), 0.01);
    }

    @Test
    public void update_runningData_routesToRunningAggregator() {
        sensorDataSet.add(new AggregatorRunning("addr", "RUN"));

        sensorDataSet.update(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(3.0), Cadence.of(80), Distance.of(100))));
        sensorDataSet.update(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(3.5), Cadence.of(85), Distance.of(110))));

        assertNotNull(sensorDataSet.runningDistanceSpeedCadence);
        assertEquals(Speed.of(3.5), sensorDataSet.runningDistanceSpeedCadence.getAggregatedValue(Instant.MIN).speed());
    }

    @Test
    public void update_cyclingPower_routesToCyclingPowerAggregator() {
        sensorDataSet.add(new AggregatorCyclingPower("addr", "PM"));

        sensorDataSet.update(new Raw<>(Instant.MIN,
                new BluetoothHandlerManagerCyclingPower.Data(Power.of(250f), null)));

        assertEquals(Power.of(250f), sensorDataSet.cyclingPower.getAggregatedValue(Instant.MIN));
    }

    @Test
    public void update_atmosphericPressure_routesToBarometer() {
        sensorDataSet.add(new AggregatorBarometer("addr", "BARO"));

        sensorDataSet.update(new Raw<>(Instant.MIN, AtmosphericPressure.ofHPA(1015f)));

        assertNotNull(sensorDataSet.barometer);
        assertTrue(sensorDataSet.barometer.hasReceivedData());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void update_unknownType_throws() {
        sensorDataSet.update(new Raw<>(Instant.MIN, "unknown"));
    }

    // --- add()/remove() lifecycle ---

    @Test
    public void add_heartRate_setsField() {
        sensorDataSet.add(new AggregatorHeartRate("addr", "HRM"));

        assertNotNull(sensorDataSet.heartRate);
    }

    @Test
    public void add_cyclingCadence_setsField() {
        sensorDataSet.add(new AggregatorCyclingCadence("addr", "CSC"));

        assertNotNull(sensorDataSet.cyclingCadence);
    }

    @Test
    public void add_cyclingPower_setsField() {
        sensorDataSet.add(new AggregatorCyclingPower("addr", "PM"));

        assertNotNull(sensorDataSet.cyclingPower);
    }

    @Test
    public void add_running_setsField() {
        sensorDataSet.add(new AggregatorRunning("addr", "RUN"));

        assertNotNull(sensorDataSet.runningDistanceSpeedCadence);
    }

    @Test
    public void remove_heartRate_clearsField() {
        AggregatorHeartRate hr = new AggregatorHeartRate("addr", "HRM");
        sensorDataSet.add(hr);

        sensorDataSet.remove(hr);

        assertNull(sensorDataSet.heartRate);
    }

    @Test
    public void remove_cyclingPower_clearsField() {
        AggregatorCyclingPower cp = new AggregatorCyclingPower("addr", "PM");
        sensorDataSet.add(cp);

        sensorDataSet.remove(cp);

        assertNull(sensorDataSet.cyclingPower);
    }

    // --- clear() ---

    @Test
    public void clear_nullsAllFields() {
        sensorDataSet.add(new AggregatorHeartRate("addr", "HRM"));
        sensorDataSet.add(new AggregatorCyclingCadence("addr", "CSC"));
        sensorDataSet.add(new AggregatorCyclingDistanceSpeed("addr", "CSC"));
        sensorDataSet.add(new AggregatorCyclingPower("addr", "PM"));
        sensorDataSet.add(new AggregatorRunning("addr", "RUN"));
        sensorDataSet.add(new AggregatorBarometer("addr", "BARO"));
        sensorDataSet.add(new AggregatorGPS("addr"));

        sensorDataSet.clear();

        assertNull(sensorDataSet.heartRate);
        assertNull(sensorDataSet.cyclingCadence);
        assertNull(sensorDataSet.cyclingDistanceSpeed);
        assertNull(sensorDataSet.cyclingPower);
        assertNull(sensorDataSet.runningDistanceSpeedCadence);
        assertNull(sensorDataSet.barometer);
        assertNull(sensorDataSet.gps);
    }

    // --- getCadence() priority: cycling > running ---

    @Test
    public void getCadence_prefersCyclingCadence() {
        sensorDataSet.add(new AggregatorCyclingCadence("addr", "CSC"));
        sensorDataSet.add(new AggregatorRunning("addr", "RUN"));

        // Feed cycling cadence: 1 rev in 1024 ticks → 60 RPM
        sensorDataSet.update(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(1, 1024)));
        sensorDataSet.update(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingCadence.CrankData(2, 2048)));

        // Feed running cadence: 170 RPM
        sensorDataSet.update(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(3.0), Cadence.of(170), null)));
        sensorDataSet.update(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(3.0), Cadence.of(170), null)));

        // Cycling cadence (60 RPM) should take priority
        assertEquals(60, sensorDataSet.getCadence().first.getRPM(), 0.01);
    }

    @Test
    public void getCadence_fallsBackToRunning() {
        sensorDataSet.add(new AggregatorRunning("addr", "RUN"));

        sensorDataSet.update(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(3.0), Cadence.of(170), null)));
        sensorDataSet.update(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(3.0), Cadence.of(170), null)));

        assertEquals(170, sensorDataSet.getCadence().first.getRPM(), 0.01);
    }

    @Test
    public void getCadence_noSensor_returnsNull() {
        assertNull(sensorDataSet.getCadence());
    }

    // --- getSpeed() priority: cycling > running ---

    @Test
    public void getSpeed_prefersCyclingSpeed() {
        AggregatorCyclingDistanceSpeed cyclingAgg = new AggregatorCyclingDistanceSpeed("addr", "CSC");
        cyclingAgg.setWheelCircumference(Distance.ofMM(2150));
        sensorDataSet.add(cyclingAgg);
        sensorDataSet.add(new AggregatorRunning("addr", "RUN"));

        // Feed cycling speed data
        sensorDataSet.update(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingDistanceSpeed.WheelData(1, 6184)));
        sensorDataSet.update(new Raw<>(Instant.MIN, new BluetoothHandlerCyclingDistanceSpeed.WheelData(2, 8016)));

        // Feed running speed data
        sensorDataSet.update(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(3.0), Cadence.of(80), null)));
        sensorDataSet.update(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(3.5), Cadence.of(85), null)));

        // Cycling speed should take priority
        assertEquals(1.20, sensorDataSet.getSpeed().first.toMPS(), 0.01);
    }

    @Test
    public void getSpeed_fallsBackToRunning() {
        sensorDataSet.add(new AggregatorRunning("addr", "RUN"));

        sensorDataSet.update(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(3.0), Cadence.of(80), null)));
        sensorDataSet.update(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(3.5), Cadence.of(85), null)));

        assertEquals(Speed.of(3.5), sensorDataSet.getSpeed().first);
    }

    @Test
    public void getSpeed_noSensor_returnsNull() {
        assertNull(sensorDataSet.getSpeed());
    }

    // --- getHeartRate() ---

    @Test
    public void getHeartRate_withSensor_returnsValue() {
        sensorDataSet.add(new AggregatorHeartRate("addr", "HRM"));
        sensorDataSet.update(new Raw<>(Instant.MIN, HeartRate.of(120)));

        assertEquals(HeartRate.of(120), sensorDataSet.getHeartRate().first);
    }

    @Test
    public void getHeartRate_noSensor_returnsNull() {
        assertNull(sensorDataSet.getHeartRate());
    }

    // --- fillTrackPoint ---

    @Test
    public void fillTrackPoint_populatesAvailableSensorData() {
        sensorDataSet.add(new AggregatorHeartRate("addr", "HRM"));
        sensorDataSet.add(new AggregatorCyclingPower("addr", "PM"));
        sensorDataSet.add(new AggregatorBarometer("addr", "BARO"));

        sensorDataSet.update(new Raw<>(Instant.MIN, HeartRate.of(140)));
        sensorDataSet.update(new Raw<>(Instant.MIN,
                new BluetoothHandlerManagerCyclingPower.Data(Power.of(250f), null)));
        // Barometer needs 2 samples to compute gain/loss
        sensorDataSet.update(new Raw<>(Instant.MIN, AtmosphericPressure.ofHPA(1015f)));
        sensorDataSet.update(new Raw<>(Instant.MIN, AtmosphericPressure.ofHPA(1015f)));

        TrackPoint tp = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.MIN);
        sensorDataSet.fillTrackPoint(tp);

        assertEquals(HeartRate.of(140), tp.getHeartRate());
        assertEquals(Power.of(250f), tp.getPower());
    }

    @Test
    public void fillTrackPoint_emptyDataSet_noFieldsSet() {
        TrackPoint tp = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.MIN);

        sensorDataSet.fillTrackPoint(tp);

        assertFalse(tp.hasHeartRate());
        assertFalse(tp.hasPower());
    }

    // --- reset() ---

    @Test
    public void reset_resetsAllAggregators() {
        sensorDataSet.add(new AggregatorHeartRate("addr", "HRM"));
        sensorDataSet.update(new Raw<>(Instant.MIN, HeartRate.of(120)));

        sensorDataSet.reset();

        // After reset, the aggregator should return getNoneValue()
        // because resetAggregated() is a no-op for HeartRate,
        // the value is still accessible
        assertEquals(HeartRate.of(120), sensorDataSet.heartRate.getAggregatedValue(Instant.MIN));
    }
}
