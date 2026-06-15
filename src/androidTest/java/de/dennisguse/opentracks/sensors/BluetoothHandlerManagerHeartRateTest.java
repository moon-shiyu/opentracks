package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
    public void parseHeartRate_emptyPayload_returnsNull() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerManagerHeartRate.HEARTRATE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{});

        assertNull(BluetoothHandlerManagerHeartRate.parseHeartRate(characteristic));
    }

    @Test
    public void parseHeartRate_uint8Format_insufficientBytes_returnsNull() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerManagerHeartRate.HEARTRATE.serviceUUID(), 0, 0);
        // Flags byte indicates UINT8 format but no data byte follows
        characteristic.setValue(new byte[]{0x00});

        assertNull(BluetoothHandlerManagerHeartRate.parseHeartRate(characteristic));
    }

    @Test
    public void parseHeartRate_uint16Format_insufficientBytes_returnsNull() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerManagerHeartRate.HEARTRATE.serviceUUID(), 0, 0);
        // Flags byte indicates UINT16 format but only 1 data byte follows (need 2)
        characteristic.setValue(new byte[]{0x01, 0x01});

        assertNull(BluetoothHandlerManagerHeartRate.parseHeartRate(characteristic));
    }
}