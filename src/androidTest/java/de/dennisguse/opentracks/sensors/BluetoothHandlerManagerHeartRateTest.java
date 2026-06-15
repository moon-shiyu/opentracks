package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Test;

import de.dennisguse.opentracks.data.models.HeartRate;

public class BluetoothHandlerManagerHeartRateTest {

    @Test
    public void parseHeartRate_uint8() {
        // given
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerManagerHeartRate.HEARTRATE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x02, 0x3C});

        // when
        HeartRate heartRate = BluetoothHandlerManagerHeartRate.parseHeartRate(characteristic);

        // then
        assertEquals(HeartRate.of(60), heartRate);
    }

    @Test
    public void parseHeartRate_uint16() {
        // given
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerManagerHeartRate.HEARTRATE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x01, 0x01, 0x01});

        // when
        HeartRate heartRate = BluetoothHandlerManagerHeartRate.parseHeartRate(characteristic);

        // then
        assertEquals(HeartRate.of(257), heartRate);
    }

    @Test
    public void handlePayload_emitsHeartRate() {
        // given
        RecordingSensorDataObserver observer = new RecordingSensorDataObserver();
        BluetoothHandlerManagerHeartRate subject = new BluetoothHandlerManagerHeartRate();
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerManagerHeartRate.HEARTRATE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x02, 0x3C});

        // when
        subject.handlePayload(observer, BluetoothHandlerManagerHeartRate.HEARTRATE, "name", "address", characteristic);

        // then
        assertEquals(1, observer.changes.size());
        assertEquals(HeartRate.of(60), observer.changes.get(0).value());
    }

    @Test
    public void handlePayload_emptyDoesNotEmit() {
        // given
        RecordingSensorDataObserver observer = new RecordingSensorDataObserver();
        BluetoothHandlerManagerHeartRate subject = new BluetoothHandlerManagerHeartRate();
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerManagerHeartRate.HEARTRATE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{});

        // when
        subject.handlePayload(observer, BluetoothHandlerManagerHeartRate.HEARTRATE, "name", "address", characteristic);

        // then
        assertTrue(observer.changes.isEmpty());
    }
}