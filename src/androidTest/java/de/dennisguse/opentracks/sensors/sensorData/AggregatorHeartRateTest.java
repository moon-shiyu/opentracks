package de.dennisguse.opentracks.sensors.sensorData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;

import de.dennisguse.opentracks.data.models.HeartRate;

public class AggregatorHeartRateTest {

    @Test
    public void computeValue_passthrough() {
        AggregatorHeartRate agg = new AggregatorHeartRate("addr", "name");

        agg.add(new Raw<>(Instant.MIN, HeartRate.of(120)));

        assertEquals(HeartRate.of(120), agg.getAggregatedValue(Instant.MIN));
    }

    @Test
    public void computeValue_overwritesPrevious() {
        AggregatorHeartRate agg = new AggregatorHeartRate("addr", "name");

        agg.add(new Raw<>(Instant.MIN, HeartRate.of(120)));
        agg.add(new Raw<>(Instant.MIN, HeartRate.of(130)));

        assertEquals(HeartRate.of(130), agg.getAggregatedValue(Instant.MIN));
    }

    @Test
    public void hasReceivedData_falseInitially() {
        AggregatorHeartRate agg = new AggregatorHeartRate("addr", "name");

        assertFalse(agg.hasReceivedData());
    }

    @Test
    public void hasReceivedData_trueAfterAdd() {
        AggregatorHeartRate agg = new AggregatorHeartRate("addr", "name");

        agg.add(new Raw<>(Instant.MIN, HeartRate.of(120)));

        assertTrue(agg.hasReceivedData());
    }

    @Test
    public void resetAggregated_noOp() {
        AggregatorHeartRate agg = new AggregatorHeartRate("addr", "name");
        agg.add(new Raw<>(Instant.MIN, HeartRate.of(120)));

        agg.resetAggregated();

        assertEquals(HeartRate.of(120), agg.getAggregatedValue(Instant.MIN));
    }

    @Test
    public void getSensorNameOrAddress_returnsName() {
        AggregatorHeartRate agg = new AggregatorHeartRate("AA:BB:CC", "MyHRM");

        assertEquals("MyHRM", agg.getSensorNameOrAddress());
    }

    @Test
    public void getSensorNameOrAddress_fallsBackToAddress() {
        AggregatorHeartRate agg = new AggregatorHeartRate("AA:BB:CC", null);

        assertEquals("AA:BB:CC", agg.getSensorNameOrAddress());
    }
}
