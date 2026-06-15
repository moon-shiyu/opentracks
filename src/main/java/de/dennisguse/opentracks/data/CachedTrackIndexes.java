package de.dennisguse.opentracks.data;

import android.database.Cursor;

import de.dennisguse.opentracks.data.tables.TracksColumns;

/**
 * A cache of track column indexes for use with {@link ContentProviderUtils#createTrack(Cursor)}.
 */
class CachedTrackIndexes {
    final int idIndex;
    final int uuidIndex;
    final int nameIndex;
    final int descriptionIndex;
    final int activityTypeIndex;
    final int activityTypeLocalizedIndex;
    final int startTimeIndex;
    final int startTimeOffsetIndex;
    final int stopTimeIndex;
    final int totalDistanceIndex;
    final int totalTimeIndex;
    final int movingTimeIndex;
    final int maxSpeedIndex;
    final int minAltitudeIndex;
    final int maxAltitudeIndex;
    final int altitudeGainIndex;
    final int altitudeLossIndex;

    CachedTrackIndexes(Cursor cursor) {
        idIndex = cursor.getColumnIndexOrThrow(TracksColumns._ID);
        uuidIndex = cursor.getColumnIndexOrThrow(TracksColumns.UUID);
        nameIndex = cursor.getColumnIndexOrThrow(TracksColumns.NAME);
        descriptionIndex = cursor.getColumnIndexOrThrow(TracksColumns.DESCRIPTION);
        activityTypeIndex = cursor.getColumnIndexOrThrow(TracksColumns.ACTIVITY_TYPE);
        activityTypeLocalizedIndex = cursor.getColumnIndexOrThrow(TracksColumns.ACTIVITY_TYPE_LOCALIZED);
        startTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME);
        startTimeOffsetIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME_OFFSET);
        stopTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STOPTIME);
        totalDistanceIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE);
        totalTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALTIME);
        movingTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.MOVINGTIME);
        maxSpeedIndex = cursor.getColumnIndexOrThrow(TracksColumns.MAXSPEED);
        minAltitudeIndex = cursor.getColumnIndexOrThrow(TracksColumns.MIN_ALTITUDE);
        maxAltitudeIndex = cursor.getColumnIndexOrThrow(TracksColumns.MAX_ALTITUDE);
        altitudeGainIndex = cursor.getColumnIndexOrThrow(TracksColumns.ALTITUDE_GAIN);
        altitudeLossIndex = cursor.getColumnIndexOrThrow(TracksColumns.ALTITUDE_LOSS);
    }
}
