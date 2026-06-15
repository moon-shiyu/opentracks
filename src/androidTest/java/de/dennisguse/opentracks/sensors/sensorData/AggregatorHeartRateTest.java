package de.dennisguse.opentracks.sensors.sensorData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;

import de.dennisguse.opentracks.data.models.HeartRate;

public class AggregatorHeartRateTest {

    @Test
    public void getAggregatedValue_returnsLatestSample() {
        AggregatorHeartRate subject = new AggregatorHeartRate("address", "name");

        // when
        subject.add(new Raw<>(Instant.MIN, HeartRate.of(60)));
        subject.add(new Raw<>(Instant.MIN, HeartRate.of(80)));

        // then: heart rate is an instantaneous value, the most recent sample wins
        assertTrue(subject.hasReceivedData());
        assertEquals(HeartRate.of(80), subject.getAggregatedValue(Instant.MIN));
    }

    @Test
    public void getAggregatedValue_noData_isZero() {
        AggregatorHeartRate subject = new AggregatorHeartRate("address", "name");

        // then
        assertFalse(subject.hasReceivedData());
        assertEquals(HeartRate.of(0), subject.getAggregatedValue(Instant.MIN));
    }
}
