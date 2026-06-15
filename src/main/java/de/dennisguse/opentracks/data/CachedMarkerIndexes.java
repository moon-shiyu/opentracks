package de.dennisguse.opentracks.data;

import android.database.Cursor;

import de.dennisguse.opentracks.data.tables.MarkerColumns;

/**
 * A cache of marker column indexes for use with {@link ContentProviderUtils#createMarker(Cursor)}.
 */
class CachedMarkerIndexes {
    final int idIndex;
    final int nameIndex;
    final int descriptionIndex;
    final int categoryIndex;
    final int iconIndex;
    final int trackIdIndex;
    final int longitudeIndex;
    final int latitudeIndex;
    final int timeIndex;
    final int altitudeIndex;
    final int accuracyIndex;
    final int bearingIndex;
    final int photoUrlIndex;

    CachedMarkerIndexes(Cursor cursor) {
        idIndex = cursor.getColumnIndexOrThrow(MarkerColumns._ID);
        nameIndex = cursor.getColumnIndexOrThrow(MarkerColumns.NAME);
        descriptionIndex = cursor.getColumnIndexOrThrow(MarkerColumns.DESCRIPTION);
        categoryIndex = cursor.getColumnIndexOrThrow(MarkerColumns.CATEGORY);
        iconIndex = cursor.getColumnIndexOrThrow(MarkerColumns.ICON);
        trackIdIndex = cursor.getColumnIndexOrThrow(MarkerColumns.TRACKID);
        longitudeIndex = cursor.getColumnIndexOrThrow(MarkerColumns.LONGITUDE);
        latitudeIndex = cursor.getColumnIndexOrThrow(MarkerColumns.LATITUDE);
        timeIndex = cursor.getColumnIndexOrThrow(MarkerColumns.TIME);
        altitudeIndex = cursor.getColumnIndexOrThrow(MarkerColumns.ALTITUDE);
        accuracyIndex = cursor.getColumnIndexOrThrow(MarkerColumns.ACCURACY);
        bearingIndex = cursor.getColumnIndexOrThrow(MarkerColumns.BEARING);
        photoUrlIndex = cursor.getColumnIndexOrThrow(MarkerColumns.PHOTOURL);
    }
}
