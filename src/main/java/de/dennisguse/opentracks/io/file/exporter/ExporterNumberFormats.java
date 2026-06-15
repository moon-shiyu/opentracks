package de.dennisguse.opentracks.io.file.exporter;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Shared {@link NumberFormat} definitions used by multiple track exporters (GPX, CSV).
 * <p>
 * All formats use {@link Locale#US} and have grouping disabled,
 * as GPS/track data readers expect US-style decimal punctuation.
 * <p>
 * NOTE: {@code COORDINATE_FORMAT} and {@code DISTANCE_FORMAT} are NOT shared here
 * because GPX and CSV exporters configure them differently.
 */
public final class ExporterNumberFormats {

    private ExporterNumberFormats() {}

    public static final NumberFormat ALTITUDE_FORMAT = NumberFormat.getInstance(Locale.US);
    public static final NumberFormat SPEED_FORMAT = NumberFormat.getInstance(Locale.US);
    public static final NumberFormat HEARTRATE_FORMAT = NumberFormat.getInstance(Locale.US);
    public static final NumberFormat CADENCE_FORMAT = NumberFormat.getInstance(Locale.US);
    public static final NumberFormat POWER_FORMAT = NumberFormat.getInstance(Locale.US);

    static {
        ALTITUDE_FORMAT.setMaximumFractionDigits(1);
        ALTITUDE_FORMAT.setGroupingUsed(false);

        SPEED_FORMAT.setMaximumFractionDigits(2);
        SPEED_FORMAT.setGroupingUsed(false);

        HEARTRATE_FORMAT.setMaximumFractionDigits(0);
        HEARTRATE_FORMAT.setGroupingUsed(false);

        CADENCE_FORMAT.setMaximumFractionDigits(0);
        CADENCE_FORMAT.setGroupingUsed(false);

        POWER_FORMAT.setMaximumFractionDigits(0);
        POWER_FORMAT.setGroupingUsed(false);
    }
}
