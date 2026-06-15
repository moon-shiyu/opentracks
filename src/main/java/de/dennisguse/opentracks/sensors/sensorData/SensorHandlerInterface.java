package de.dennisguse.opentracks.sensors.sensorData;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.List;

import de.dennisguse.opentracks.sensors.SensorManager;
import de.dennisguse.opentracks.sensors.ServiceMeasurementUUID;

public interface SensorHandlerInterface {

    List<ServiceMeasurementUUID> getServices();

    Aggregator<?, ?> createEmptySensorData(String address, String name);

    void handlePayload(SensorManager.SensorDataChangedObserver observer, ServiceMeasurementUUID serviceMeasurementUUID, String sensorName, String address, BluetoothGattCharacteristic characteristic);

    /**
     * Single seam between protocol parsing and aggregation: wraps a freshly parsed sensor value into a
     * timestamped {@link Raw} event and forwards it to the aggregation layer.
     * <p>
     * Each handler stays in control of <em>whether</em> and <em>when</em> to emit (e.g., its own null
     * handling); this only removes the repeated {@code observer.onChange(new Raw<>(observer.getNow(), …))}
     * boilerplate.
     */
    default <T> void emit(SensorManager.SensorDataChangedObserver observer, T value) {
        observer.onChange(new Raw<>(observer.getNow(), value));
    }
}
