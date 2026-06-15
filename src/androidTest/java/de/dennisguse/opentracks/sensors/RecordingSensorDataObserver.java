package de.dennisguse.opentracks.sensors;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.sensors.sensorData.Aggregator;
import de.dennisguse.opentracks.sensors.sensorData.Raw;

/**
 * Test double for {@link SensorManager.SensorDataChangedObserver}.
 * <p>
 * Records every {@link Raw} event forwarded through {@link #onChange(Raw)} so a {@code BluetoothHandler*}
 * can be exercised end-to-end (parse + emit) in isolation, without a real Bluetooth connection or
 * {@link SensorDataSet}. {@link #getNow()} returns a fixed instant for deterministic timestamps.
 */
class RecordingSensorDataObserver implements SensorManager.SensorDataChangedObserver {

    private final Instant now;

    final List<Raw<?>> changes = new ArrayList<>();

    RecordingSensorDataObserver() {
        this(Instant.parse("2020-01-01T00:00:00Z"));
    }

    RecordingSensorDataObserver(@NonNull Instant now) {
        this.now = now;
    }

    @Override
    public void onConnect(Aggregator<?, ?> sensorData) {
    }

    @Override
    public void onChange(Raw<?> sensorData) {
        changes.add(sensorData);
    }

    @Override
    public void onDisconnect(Aggregator<?, ?> sensorData) {
    }

    @Override
    public void onRemove(Aggregator<?, ?> sensorData) {
    }

    @Override
    public Instant getNow() {
        return now;
    }
}
