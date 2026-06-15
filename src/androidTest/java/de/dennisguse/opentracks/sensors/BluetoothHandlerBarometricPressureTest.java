package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    public void handlePayload_emitsPressure() {
        // given
        RecordingSensorDataObserver observer = new RecordingSensorDataObserver();
        BluetoothHandlerBarometricPressure subject = new BluetoothHandlerBarometricPressure();
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerBarometricPressure.BAROMETRIC_PRESSURE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{(byte) 0xB2, (byte) 0x48, (byte) 0x0F, (byte) 0x00});

        // when
        subject.handlePayload(observer, BluetoothHandlerBarometricPressure.BAROMETRIC_PRESSURE, "name", "address", characteristic);

        // then
        assertEquals(1, observer.changes.size());
        assertEquals(AtmosphericPressure.ofHPA(1001.65f), observer.changes.get(0).value());
    }

    @Test
    public void handlePayload_tooShortDoesNotEmit() {
        // given: fewer than the 4 bytes required by the pressure characteristic
        RecordingSensorDataObserver observer = new RecordingSensorDataObserver();
        BluetoothHandlerBarometricPressure subject = new BluetoothHandlerBarometricPressure();
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerBarometricPressure.BAROMETRIC_PRESSURE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{0x00, 0x00, 0x00});

        // when
        subject.handlePayload(observer, BluetoothHandlerBarometricPressure.BAROMETRIC_PRESSURE, "name", "address", characteristic);

        // then
        assertTrue(observer.changes.isEmpty());
    }
}