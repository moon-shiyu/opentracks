package de.dennisguse.opentracks.sensors.sensorData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.sensors.BluetoothHandlerRunningSpeedAndCadence;

public class AggregatorRunningTest {

    @Test
    public void computeValue_requiresTwoSamples() {
        AggregatorRunning agg = new AggregatorRunning("addr", "name");

        // First sample is stored as previous but computeValue returns early (previous == null guard)
        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(
                        Speed.of(3.0), Cadence.of(80), Distance.of(100))));

        assertFalse(agg.hasReceivedData());
    }

    @Test
    public void computeValue_accumulatesDistance() {
        AggregatorRunning agg = new AggregatorRunning("addr", "name");

        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(
                        Speed.of(3.0), Cadence.of(80), Distance.of(100))));
        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(
                        Speed.of(3.5), Cadence.of(85), Distance.of(110))));

        AggregatorRunning.Data result = agg.getAggregatedValue(Instant.MIN);
        assertNotNull(result);
        assertEquals(Speed.of(3.5), result.speed());
        assertEquals(Cadence.of(85), result.cadence());
        assertEquals(Distance.of(10), result.distance());
    }

    @Test
    public void computeValue_nullTotalDistance_noAccumulation() {
        AggregatorRunning agg = new AggregatorRunning("addr", "name");

        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(
                        Speed.of(3.0), Cadence.of(80), null)));
        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(
                        Speed.of(3.5), Cadence.of(85), null)));

        AggregatorRunning.Data result = agg.getAggregatedValue(Instant.MIN);
        assertNotNull(result);
        assertEquals(Speed.of(3.5), result.speed());
        assertEquals(Cadence.of(85), result.cadence());
        assertNull(result.distance());
    }

    @Test
    public void computeValue_accumulatesOverMultipleSamples() {
        AggregatorRunning agg = new AggregatorRunning("addr", "name");

        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(
                        Speed.of(3.0), Cadence.of(80), Distance.of(100))));
        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(
                        Speed.of(3.5), Cadence.of(85), Distance.of(110))));
        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(
                        Speed.of(4.0), Cadence.of(90), Distance.of(125))));

        AggregatorRunning.Data result = agg.getAggregatedValue(Instant.MIN);
        assertEquals(Speed.of(4.0), result.speed());
        assertEquals(Cadence.of(90), result.cadence());
        // 10 + 15 = 25
        assertEquals(Distance.of(25), result.distance());
    }

    @Test
    public void resetAggregated_clearsDistance_keepsSpeedCadence() {
        AggregatorRunning agg = new AggregatorRunning("addr", "name");

        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(
                        Speed.of(3.0), Cadence.of(80), Distance.of(100))));
        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(
                        Speed.of(3.5), Cadence.of(85), Distance.of(110))));

        agg.resetAggregated();

        AggregatorRunning.Data result = agg.getAggregatedValue(Instant.MIN);
        assertEquals(Speed.of(3.5), result.speed());
        assertEquals(Cadence.of(85), result.cadence());
        assertEquals(Distance.of(0), result.distance());
    }

    @Test
    public void hasReceivedData_falseInitially() {
        AggregatorRunning agg = new AggregatorRunning("addr", "name");

        assertFalse(agg.hasReceivedData());
    }

    @Test
    public void hasReceivedData_trueAfterTwoSamples() {
        AggregatorRunning agg = new AggregatorRunning("addr", "name");

        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(
                        Speed.of(3.0), Cadence.of(80), Distance.of(100))));
        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerRunningSpeedAndCadence.Data(
                        Speed.of(3.5), Cadence.of(85), Distance.of(110))));

        assertTrue(agg.hasReceivedData());
    }

    @Test
    public void getSensorNameOrAddress_returnsName() {
        AggregatorRunning agg = new AggregatorRunning("AA:BB:CC", "MyRunner");

        assertEquals("MyRunner", agg.getSensorNameOrAddress());
    }
}
