package de.dennisguse.opentracks.sensors.sensorData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.time.Instant;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.sensors.BluetoothHandlerRunningSpeedAndCadence;

public class AggregatorRunningTest {

    private static Raw<BluetoothHandlerRunningSpeedAndCadence.Data> raw(float speedMps, int cadenceRpm, double totalDistanceM) {
        return new Raw<>(Instant.MIN, new BluetoothHandlerRunningSpeedAndCadence.Data(Speed.of(speedMps), Cadence.of(cadenceRpm), Distance.of(totalDistanceM)));
    }

    @Test
    public void computeValue_needsTwoSamplesForDistance() {
        AggregatorRunning subject = new AggregatorRunning("address", "name");

        // when: only one sample
        subject.add(raw(3f, 80, 100));

        // then: distance is derived from the delta between samples, so a single sample yields nothing yet
        assertFalse(subject.hasReceivedData());
    }

    @Test
    public void computeValue_accumulatesDistanceAndPassesThroughSpeedCadence() {
        AggregatorRunning subject = new AggregatorRunning("address", "name");

        // when
        subject.add(raw(3f, 80, 100));
        subject.add(raw(4f, 90, 130)); // +30m
        subject.add(raw(5f, 100, 150)); // +20m

        // then: speed/cadence reflect the latest sample; distance accumulates the per-sample deltas
        AggregatorRunning.Data value = subject.getAggregatedValue(Instant.MIN);
        assertEquals(Speed.of(5), value.speed());
        assertEquals(Cadence.of(100), value.cadence());
        assertEquals(50, value.distance().toM(), 0.01);
    }

    @Test
    public void resetAggregated_zeroesDistanceButKeepsSpeedCadence() {
        AggregatorRunning subject = new AggregatorRunning("address", "name");
        subject.add(raw(3f, 80, 100));
        subject.add(raw(4f, 90, 130));

        // when
        subject.resetAggregated();

        // then: only the long-term distance is reset; the instantaneous speed/cadence are retained
        AggregatorRunning.Data value = subject.getAggregatedValue(Instant.MIN);
        assertEquals(Speed.of(4), value.speed());
        assertEquals(Cadence.of(90), value.cadence());
        assertEquals(0, value.distance().toM(), 0.01);
    }
}
