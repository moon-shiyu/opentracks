package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Test;

/**
 * Cadence can be derived from two different BLE services (Cycling Power and Cycling Speed and Cadence).
 * These tests lock in {@link BluetoothHandlerCyclingCadence#handlePayload}'s routing: it must extract the
 * crank component from whichever service delivered the payload and emit nothing when no crank data is present.
 */
public class BluetoothHandlerCyclingCadenceTest {

    @Test
    public void handlePayload_fromCyclingPowerService_emitsCrank() {
        // given: a Cycling Power payload that also carries crank revolution data
        RecordingSensorDataObserver observer = new RecordingSensorDataObserver();
        BluetoothHandlerCyclingCadence subject = new BluetoothHandlerCyclingCadence();
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerManagerCyclingPower.CYCLING_POWER.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x2C, 0x00, 0x00, 0x00, (byte) 0x9F, 0x00, 0x0C, 0x00, (byte) 0xE5, 0x42});

        // when
        subject.handlePayload(observer, BluetoothHandlerManagerCyclingPower.CYCLING_POWER, "name", "address", characteristic);

        // then
        assertEquals(1, observer.changes.size());
        assertTrue(observer.changes.get(0).value() instanceof BluetoothHandlerCyclingCadence.CrankData);
        BluetoothHandlerCyclingCadence.CrankData crank = (BluetoothHandlerCyclingCadence.CrankData) observer.changes.get(0).value();
        assertEquals(12, crank.crankRevolutionsCount());
        assertEquals(17125, crank.crankRevolutionsTime());
    }

    @Test
    public void handlePayload_fromCscService_emitsCrank() {
        // given: a Cycling Speed and Cadence payload with crank only (flags = 0x02)
        RecordingSensorDataObserver observer = new RecordingSensorDataObserver();
        BluetoothHandlerCyclingCadence subject = new BluetoothHandlerCyclingCadence();
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x02, (byte) 0xC8, 0x00, 0x00, 0x00, 0x06, (byte) 0x99});

        // when
        subject.handlePayload(observer, BluetoothHandlerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE, "name", "address", characteristic);

        // then
        assertEquals(1, observer.changes.size());
        assertTrue(observer.changes.get(0).value() instanceof BluetoothHandlerCyclingCadence.CrankData);
        BluetoothHandlerCyclingCadence.CrankData crank = (BluetoothHandlerCyclingCadence.CrankData) observer.changes.get(0).value();
        assertEquals(200, crank.crankRevolutionsCount());
    }

    @Test
    public void handlePayload_fromCscService_wheelOnly_doesNotEmit() {
        // given: a Cycling Speed and Cadence payload with wheel only (flags = 0x01); no crank component
        RecordingSensorDataObserver observer = new RecordingSensorDataObserver();
        BluetoothHandlerCyclingCadence subject = new BluetoothHandlerCyclingCadence();
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x01, (byte) 0xFF, (byte) 0xFF, 0, 1, 0x45, (byte) 0x99});

        // when
        subject.handlePayload(observer, BluetoothHandlerCyclingDistanceSpeed.CYCLING_SPEED_CADENCE, "name", "address", characteristic);

        // then
        assertTrue(observer.changes.isEmpty());
    }
}
