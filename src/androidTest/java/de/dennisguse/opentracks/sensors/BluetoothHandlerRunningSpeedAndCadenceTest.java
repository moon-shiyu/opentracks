package de.dennisguse.opentracks.sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Test;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;

public class BluetoothHandlerRunningSpeedAndCadenceTest {

    @Test
    public void parseRunningSpeedAndCadence_with_distance() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerRunningSpeedAndCadence.RUNNING_SPEED_CADENCE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{2, 0, 5, 80, (byte) 0xFF, (byte) 0xFF, 0, 1});

        // when
        BluetoothHandlerRunningSpeedAndCadence.Data sensor = BluetoothHandlerRunningSpeedAndCadence.parseRunningSpeedAndCadence("sensorName", characteristic);

        // then
        assertEquals(Speed.of(5), sensor.speed());
        assertEquals(Cadence.of(80), sensor.cadence());
        assertEquals(Distance.of(6553.5 + 1677721.6), sensor.totalDistance());
    }

    @Test
    public void parseRunningSpeedAndCadence_emptyPayload_returnsNull() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerRunningSpeedAndCadence.RUNNING_SPEED_CADENCE.serviceUUID(), 0, 0);
        characteristic.setValue(new byte[]{});

        assertNull(BluetoothHandlerRunningSpeedAndCadence.parseRunningSpeedAndCadence("sensor", characteristic));
    }

    @Test
    public void parseRunningSpeedAndCadence_speedOnly() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerRunningSpeedAndCadence.RUNNING_SPEED_CADENCE.serviceUUID(), 0, 0);
        // Flags=0x00 (no optional fields), speed=256 (1.0 m/s after /256), cadence=80
        characteristic.setValue(new byte[]{0x00, 0x00, 0x01, 80});

        BluetoothHandlerRunningSpeedAndCadence.Data data = BluetoothHandlerRunningSpeedAndCadence.parseRunningSpeedAndCadence("sensor", characteristic);

        assertNotNull(data);
        assertEquals(Speed.of(1.0), data.speed());
        assertEquals(Cadence.of(80), data.cadence());
        assertNull(data.totalDistance());
    }

    @Test
    public void parseRunningSpeedAndCadence_tickrX_halvesCadence() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(BluetoothHandlerRunningSpeedAndCadence.RUNNING_SPEED_CADENCE.serviceUUID(), 0, 0);
        // Flags=0x00, speed=256 (1.0 m/s), cadence=160 (SPM) → should be halved to 80 RPM
        characteristic.setValue(new byte[]{0x00, 0x00, 0x01, (byte) 160});

        BluetoothHandlerRunningSpeedAndCadence.Data data = BluetoothHandlerRunningSpeedAndCadence.parseRunningSpeedAndCadence("TICKR X-1234", characteristic);

        assertNotNull(data);
        assertEquals(Cadence.of(80), data.cadence());
    }
}