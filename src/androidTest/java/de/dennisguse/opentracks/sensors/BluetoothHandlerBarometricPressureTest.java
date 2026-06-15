package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Test;

import de.dennisguse.opentracks.data.models.AtmosphericPressure;

public class BluetoothHandlerBarometricPressureTest {

    @Test
    public void parseEnvironmentalSensing_Pa() {
        // given
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerBarometricPressure.BAROMETRIC_PRESSURE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{(byte) 0xB2, (byte) 0x48, (byte) 0x0F, (byte) 0x00});

        // when
        AtmosphericPressure pressure = BluetoothHandlerBarometricPressure.parseEnvironmentalSensing(characteristic);

        // then
        assertEquals(AtmosphericPressure.ofHPA(1001.65f), pressure);
    }

    @Test
    public void parseEnvironmentalSensing_tooShort_returnsNull() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerBarometricPressure.BAROMETRIC_PRESSURE.serviceUUID(), 0, 0);
        // Only 3 bytes, need 4
        characteristic.setValue(new byte[]{0x01, 0x02, 0x03});

        assertNull(BluetoothHandlerBarometricPressure.parseEnvironmentalSensing(characteristic));
    }

    @Test
    public void parseEnvironmentalSensing_emptyPayload_returnsNull() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerBarometricPressure.BAROMETRIC_PRESSURE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{});

        assertNull(BluetoothHandlerBarometricPressure.parseEnvironmentalSensing(characteristic));
    }

    @Test
    public void parseEnvironmentalSensing_zeroPressure() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerBarometricPressure.BAROMETRIC_PRESSURE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x00, 0x00, 0x00, 0x00});

        AtmosphericPressure pressure = BluetoothHandlerBarometricPressure.parseEnvironmentalSensing(characteristic);

        assertEquals(AtmosphericPressure.ofPA(0f), pressure);
    }
}