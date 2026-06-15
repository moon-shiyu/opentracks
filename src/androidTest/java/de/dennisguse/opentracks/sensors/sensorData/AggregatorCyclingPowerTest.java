package de.dennisguse.opentracks.sensors.sensorData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;

import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.sensors.BluetoothHandlerManagerCyclingPower;

public class AggregatorCyclingPowerTest {

    @Test
    public void computeValue_passthrough() {
        AggregatorCyclingPower agg = new AggregatorCyclingPower("addr", "name");

        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerManagerCyclingPower.Data(Power.of(250f), null)));

        assertEquals(Power.of(250f), agg.getAggregatedValue(Instant.MIN));
    }

    @Test
    public void computeValue_overwritesPrevious() {
        AggregatorCyclingPower agg = new AggregatorCyclingPower("addr", "name");

        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerManagerCyclingPower.Data(Power.of(250f), null)));
        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerManagerCyclingPower.Data(Power.of(300f), null)));

        assertEquals(Power.of(300f), agg.getAggregatedValue(Instant.MIN));
    }

    @Test
    public void hasReceivedData_falseInitially() {
        AggregatorCyclingPower agg = new AggregatorCyclingPower("addr", "name");

        assertFalse(agg.hasReceivedData());
    }

    @Test
    public void hasReceivedData_trueAfterAdd() {
        AggregatorCyclingPower agg = new AggregatorCyclingPower("addr", "name");

        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerManagerCyclingPower.Data(Power.of(250f), null)));

        assertTrue(agg.hasReceivedData());
    }

    @Test
    public void resetAggregated_noOp() {
        AggregatorCyclingPower agg = new AggregatorCyclingPower("addr", "name");
        agg.add(new Raw<>(Instant.MIN,
                new BluetoothHandlerManagerCyclingPower.Data(Power.of(250f), null)));

        agg.resetAggregated();

        assertEquals(Power.of(250f), agg.getAggregatedValue(Instant.MIN));
    }

    @Test
    public void getSensorNameOrAddress_returnsName() {
        AggregatorCyclingPower agg = new AggregatorCyclingPower("AA:BB:CC", "MyPowerMeter");

        assertEquals("MyPowerMeter", agg.getSensorNameOrAddress());
    }

    @Test
    public void getSensorNameOrAddress_fallsBackToAddress() {
        AggregatorCyclingPower agg = new AggregatorCyclingPower("AA:BB:CC", null);

        assertEquals("AA:BB:CC", agg.getSensorNameOrAddress());
    }
}
