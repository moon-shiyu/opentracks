package de.dennisguse.opentracks.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.dennisguse.opentracks.sensors.sensorData.Raw;
import de.dennisguse.opentracks.sensors.sensorData.SensorHandlerInterface;

/**
 * Abstract base for BLE sensor handlers that follow the common parse-then-dispatch flow.
 * <p>
 * Subclasses only need to implement {@link #parseCharacteristic(BluetoothGattCharacteristic)}
 * to convert raw BLE bytes into a typed data object. The base class handles:
 * <ul>
 *     <li>Null-checking the parse result before dispatching</li>
 *     <li>Wrapping the result in {@link Raw} with the observer's current time</li>
 *     <li>Storing sensorName/address for subclasses that need device-specific workarounds</li>
 * </ul>
 * <p>
 * Handlers with multi-service dispatch (e.g., CyclingCadence which dispatches on
 * serviceMeasurementUUID to call different parsers) should implement
 * {@link SensorHandlerInterface} directly instead of extending this class.
 */
public abstract class AbstractBluetoothHandler<T> implements SensorHandlerInterface {

    /** Sensor name from the most recent handlePayload call; available to {@link #parseCharacteristic}. */
    protected String sensorName;

    /** Device address from the most recent handlePayload call; available to {@link #parseCharacteristic}. */
    protected String address;

    @Override
    public final void handlePayload(
            SensorManager.SensorDataChangedObserver observer,
            @NonNull ServiceMeasurementUUID serviceMeasurementUUID,
            String sensorName,
            String address,
            BluetoothGattCharacteristic characteristic) {
        this.sensorName = sensorName;
        this.address = address;

        T data = parseCharacteristic(characteristic);
        if (data != null) {
            observer.onChange(new Raw<>(observer.getNow(), data));
        }
    }

    /**
     * Parse a BLE characteristic into the handler's data type.
     *
     * @return the parsed data, or null if the payload is invalid or incomplete.
     *         The {@link #sensorName} and {@link #address} fields are available
     *         for handlers that need device-specific workarounds.
     */
    @Nullable
    protected abstract T parseCharacteristic(BluetoothGattCharacteristic characteristic);
}
