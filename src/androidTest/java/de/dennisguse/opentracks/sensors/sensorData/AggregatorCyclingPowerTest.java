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
    public void getAggregatedValue_returnsInstantaneousPower() {
        AggregatorCyclingPower subject = new AggregatorCyclingPower("address", "name");

        // when
        subject.add(new Raw<>(Instant.MIN, new BluetoothHandlerManagerCyclingPower.Data(Power.of(250f), null)));

        // then: power is an instantaneous value taken straight from the payload
        assertTrue(subject.hasReceivedData());
        assertEquals(250f, subject.getAggregatedValue(Instant.MIN).getW(), 0.01);
    }

    @Test
    public void getAggregatedValue_noData_isZero() {
        AggregatorCyclingPower subject = new AggregatorCyclingPower("address", "name");

        // then
        assertFalse(subject.hasReceivedData());
        assertEquals(0f, subject.getAggregatedValue(Instant.MIN).getW(), 0.01);
    }
}
