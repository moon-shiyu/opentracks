/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.BuildConfig;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.data.tables.MarkerColumns;
import de.dennisguse.opentracks.data.tables.TrackPointsColumns;
import de.dennisguse.opentracks.data.tables.TracksColumns;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.ui.markers.MarkerUtils;
import de.dennisguse.opentracks.util.FileUtils;

/**
 * {@link ContentProviderUtils} implementation.
 *
 * @author Leif Hendrik Wilden
 */
public class ContentProviderUtils {

    private static final String TAG = ContentProviderUtils.class.getSimpleName();

    // The authority (the first part of the URI) for the app's content provider.
    @VisibleForTesting
    public static final String AUTHORITY_PACKAGE = BuildConfig.APPLICATION_ID + ".content";

    // The base URI for the app's content provider.
    public static final String CONTENT_BASE_URI = "content://" + AUTHORITY_PACKAGE;

    private static final String ID_SEPARATOR = ",";

    private final ContentResolver contentResolver;

    public interface ContentProviderSelectionInterface {
        SelectionData buildSelection();
    }

    public ContentProviderUtils(Context context) {
        contentResolver = context.getContentResolver();
    }

    @VisibleForTesting
    public ContentProviderUtils(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    /**
     * Creates a {@link Track} from a cursor.
     *
     * @param cursor the cursor pointing to the track
     */
    public static Track createTrack(Cursor cursor) {
        CachedTrackIndexes idx = new CachedTrackIndexes(cursor);

        Track track = new Track(ZoneOffset.ofTotalSeconds(cursor.getInt(idx.startTimeOffsetIndex)));
        TrackStatistics trackStatistics = track.getTrackStatistics();
        if (!cursor.isNull(idx.idIndex)) {
            track.setId(new Track.Id(cursor.getLong(idx.idIndex)));
        }
        if (!cursor.isNull(idx.uuidIndex)) {
            track.setUuid(UUIDUtils.fromBytes(cursor.getBlob(idx.uuidIndex)));
        }
        if (!cursor.isNull(idx.nameIndex)) {
            track.setName(cursor.getString(idx.nameIndex));
        }
        if (!cursor.isNull(idx.descriptionIndex)) {
            track.setDescription(cursor.getString(idx.descriptionIndex));
        }
        if (!cursor.isNull(idx.activityTypeIndex)) {
            track.setActivityType(ActivityType.findBy(cursor.getString(idx.activityTypeIndex)));
        }
        if (!cursor.isNull(idx.activityTypeLocalizedIndex)) {
            track.setActivityTypeLocalized(cursor.getString(idx.activityTypeLocalizedIndex));
        }

        if (!cursor.isNull(idx.startTimeIndex)) {
            trackStatistics.setStartTime(Instant.ofEpochMilli(cursor.getLong(idx.startTimeIndex)));
        }
        if (!cursor.isNull(idx.stopTimeIndex)) {
            trackStatistics.setStopTime(Instant.ofEpochMilli(cursor.getLong(idx.stopTimeIndex)));
        }
        if (!cursor.isNull(idx.totalDistanceIndex)) {
            trackStatistics.setTotalDistance(Distance.of(cursor.getFloat(idx.totalDistanceIndex)));
        }
        if (!cursor.isNull(idx.totalTimeIndex)) {
            trackStatistics.setTotalTime(Duration.ofMillis(cursor.getLong(idx.totalTimeIndex)));
        }
        if (!cursor.isNull(idx.movingTimeIndex)) {
            trackStatistics.setMovingTime(Duration.ofMillis(cursor.getLong(idx.movingTimeIndex)));
        }
        if (!cursor.isNull(idx.maxSpeedIndex)) {
            trackStatistics.setMaxSpeed(Speed.of(cursor.getFloat(idx.maxSpeedIndex)));
        }
        if (!cursor.isNull(idx.minAltitudeIndex)) {
            trackStatistics.setMinAltitude(cursor.getFloat(idx.minAltitudeIndex));
        }
        if (!cursor.isNull(idx.maxAltitudeIndex)) {
            trackStatistics.setMaxAltitude(cursor.getFloat(idx.maxAltitudeIndex));
        }
        if (!cursor.isNull(idx.altitudeGainIndex)) {
            trackStatistics.setTotalAltitudeGain(cursor.getFloat(idx.altitudeGainIndex));
        }
        if (!cursor.isNull(idx.altitudeLossIndex)) {
            trackStatistics.setTotalAltitudeLoss(cursor.getFloat(idx.altitudeLossIndex));
        }

        return track;
    }

    @VisibleForTesting
    public void deleteAllTracks(Context context) {
        //TODO Both calls should not be necessary
        contentResolver.delete(TrackPointsColumns.CONTENT_URI_BY_ID, null, null);
        contentResolver.delete(MarkerColumns.CONTENT_URI, null, null);

        // Delete tracks last since it triggers a database vaccum call
        contentResolver.delete(TracksColumns.CONTENT_URI, null, null);

        File dir = FileUtils.getPhotoDir(context);
        FileUtils.deleteDirectoryRecurse(dir);
    }

    public void deleteTracks(Context context, @NonNull List<Track.Id> trackIds) {
        // Delete track folder resources.
        for (Track.Id trackId : trackIds) {
            FileUtils.deleteDirectoryRecurse(FileUtils.getPhotoDir(context, trackId));
        }

        String whereClause = DbUtils.buildInClause(TracksColumns._ID, trackIds.size());
        contentResolver.delete(TracksColumns.CONTENT_URI, whereClause, DbUtils.idArgs(trackIds.stream().mapToLong(Track.Id::id).toArray()));
    }

    public void deleteTrack(Context context, @NonNull Track.Id trackId) {
        // Delete track folder resources.
        FileUtils.deleteDirectoryRecurse(FileUtils.getPhotoDir(context, trackId));
        contentResolver.delete(TracksColumns.CONTENT_URI, DbUtils.eqClause(TracksColumns._ID), DbUtils.idArgs(trackId.id()));
    }

    //TODO Only use for tests; also move to tests.
    @VisibleForTesting
    public List<Track> getTracks() {
        return DbUtils.cursorToList(getTrackCursor(null, null, TracksColumns._ID), ContentProviderUtils::createTrack);
    }

    public List<Track> getTracks(ContentProviderSelectionInterface selection) {
        SelectionData selectionData = selection.buildSelection();
        return DbUtils.cursorToList(getTrackCursor(selectionData.selection(), selectionData.selectionArgs(), TracksColumns._ID), ContentProviderUtils::createTrack);
    }

    public Cursor searchTracks(String searchQuery) {
        // Needed, because MARKER_COUNT is a virtual column and has to be explicitly requested.
        // Used only be TrackListAdapter
        final String[] PROJECTION = new String[]{
                TracksColumns._ID,
                TracksColumns.NAME,
                TracksColumns.DESCRIPTION, //TODO Needed?
                TracksColumns.ACTIVITY_TYPE,
                TracksColumns.ACTIVITY_TYPE_LOCALIZED,
                TracksColumns.STARTTIME,
                TracksColumns.STARTTIME_OFFSET,
                TracksColumns.TOTALDISTANCE,
                TracksColumns.TOTALTIME,
                TracksColumns.MARKER_COUNT,
        };

        String selection = null;
        String[] selectionArgs = null;
        final String sortOrder = TracksColumns.STARTTIME + " DESC";

        if (searchQuery != null) {
            selection = TracksColumns.NAME + " LIKE ? OR " +
                    TracksColumns.DESCRIPTION + " LIKE ? OR " +
                    TracksColumns.ACTIVITY_TYPE_LOCALIZED + " LIKE ?";
            selectionArgs = new String[]{"%" + searchQuery + "%", "%" + searchQuery + "%", "%" + searchQuery + "%"};
        }

        return contentResolver.query(TracksColumns.CONTENT_URI, PROJECTION, selection, selectionArgs, sortOrder);
    }

    public Track getTrack(@NonNull Track.Id trackId) {
        try (Cursor cursor = getTrackCursor(DbUtils.eqClause(TracksColumns._ID), DbUtils.idArgs(trackId.id()), null)) {
            if (cursor != null && cursor.moveToNext()) {
                return createTrack(cursor);
            }
        }
        return null;
    }

    public Track getTrack(@NonNull UUID trackUUID) {
        String trackUUIDsearch = UUIDUtils.toHex(trackUUID);
        try (Cursor cursor = getTrackCursor("hex(" + TracksColumns.UUID + ")=?", new String[]{trackUUIDsearch}, null)) {
            if (cursor != null && cursor.moveToNext()) {
                return createTrack(cursor);
            }
        }
        return null;
    }

    /**
     * Gets a track cursor.
     * The caller owns the returned cursor and is responsible for closing it.
     *
     * @param selection     the selection. Can be null
     * @param selectionArgs the selection arguments. Can be null
     * @param sortOrder     the sort order. Can be null
     */
    public Cursor getTrackCursor(String selection, String[] selectionArgs, String sortOrder) {
        return contentResolver.query(TracksColumns.CONTENT_URI, null, selection, selectionArgs, sortOrder);
    }

    /**
     * Inserts a track.
     * NOTE: This doesn't insert any trackPoints.
     *
     * @param track the track
     * @return the content provider URI of the inserted track.
     */
    public Track.Id insertTrack(Track track) {
        Uri uri = contentResolver.insert(TracksColumns.CONTENT_URI, createContentValues(track));
        return new Track.Id(ContentUris.parseId(uri));
    }

    /**
     * Updates a track.
     * NOTE: This doesn't update any trackPoints.
     *
     * @param track the track
     */
    public void updateTrack(Track track) {
        contentResolver.update(TracksColumns.CONTENT_URI, createContentValues(track), DbUtils.eqClause(TracksColumns._ID), DbUtils.idArgs(track.getId().id()));
    }

    private ContentValues createContentValues(Track track) {
        ContentValues values = new ContentValues();
        TrackStatistics trackStatistics = track.getTrackStatistics();

        if (track.getId() != null) {
            values.put(TracksColumns._ID, track.getId().id());
        }
        values.put(TracksColumns.UUID, UUIDUtils.toBytes(track.getUuid()));
        values.put(TracksColumns.NAME, track.getName());
        values.put(TracksColumns.DESCRIPTION, track.getDescription());
        values.put(TracksColumns.ACTIVITY_TYPE, track.getActivityType() != null ? track.getActivityType().getId() : null);
        values.put(TracksColumns.ACTIVITY_TYPE_LOCALIZED, track.getActivityTypeLocalized());
        values.put(TracksColumns.STARTTIME_OFFSET, track.getZoneOffset().getTotalSeconds());
        putStatisticsFields(values, trackStatistics);

        return values;
    }

    public void updateTrackStatistics(@NonNull Track.Id trackId, @NonNull TrackStatistics trackStatistics) {
        contentResolver.update(TracksColumns.CONTENT_URI, createContentValues(trackStatistics), DbUtils.eqClause(TracksColumns._ID), DbUtils.idArgs(trackId.id()));
    }

    private ContentValues createContentValues(TrackStatistics trackStatistics) {
        ContentValues values = new ContentValues();
        putStatisticsFields(values, trackStatistics);
        return values;
    }

    /**
     * Puts statistics fields into ContentValues.
     * Shared by both {@link #createContentValues(Track)} and {@link #createContentValues(TrackStatistics)}.
     */
    private void putStatisticsFields(ContentValues values, TrackStatistics trackStatistics) {
        if (trackStatistics.getStartTime() != null) {
            values.put(TracksColumns.STARTTIME, trackStatistics.getStartTime().toEpochMilli());
        }
        if (trackStatistics.getStopTime() != null) {
            values.put(TracksColumns.STOPTIME, trackStatistics.getStopTime().toEpochMilli());
        }
        values.put(TracksColumns.TOTALDISTANCE, trackStatistics.getTotalDistance().toM());
        values.put(TracksColumns.TOTALTIME, trackStatistics.getTotalTime().toMillis());
        values.put(TracksColumns.MOVINGTIME, trackStatistics.getMovingTime().toMillis());
        values.put(TracksColumns.AVGSPEED, trackStatistics.getAverageSpeed().toMPS());
        values.put(TracksColumns.AVGMOVINGSPEED, trackStatistics.getAverageMovingSpeed().toMPS());
        values.put(TracksColumns.MAXSPEED, trackStatistics.getMaxSpeed().toMPS());
        values.put(TracksColumns.MIN_ALTITUDE, trackStatistics.getMinAltitude());
        values.put(TracksColumns.MAX_ALTITUDE, trackStatistics.getMaxAltitude());
        values.put(TracksColumns.ALTITUDE_GAIN, trackStatistics.getTotalAltitudeGain());
        values.put(TracksColumns.ALTITUDE_LOSS, trackStatistics.getTotalAltitudeLoss());
    }

    public Marker createMarker(Cursor cursor) {
        CachedMarkerIndexes idx = new CachedMarkerIndexes(cursor);

        Double latitude = null;
        Double longitude = null;
        Altitude.WGS84 altitude = null;
        Distance horizontalAccuracy = null;
        Float bearing = null;
        if (!cursor.isNull(idx.longitudeIndex) && !cursor.isNull(idx.latitudeIndex)) {
            latitude = (((double) cursor.getInt(idx.latitudeIndex)) / 1E6);
            longitude = (((double) cursor.getInt(idx.longitudeIndex)) / 1E6);
        }
        if (!cursor.isNull(idx.altitudeIndex)) {
            altitude = Altitude.WGS84.of(cursor.getFloat(idx.altitudeIndex));
        }
        if (!cursor.isNull(idx.accuracyIndex)) {
            horizontalAccuracy = Distance.of(cursor.getFloat(idx.accuracyIndex));
        }
        if (!cursor.isNull(idx.bearingIndex)) {
            bearing = cursor.getFloat(idx.bearingIndex);
        }

        Position position = new Position(
                Instant.ofEpochMilli(cursor.getLong(idx.timeIndex)),
                latitude,
                longitude,
                horizontalAccuracy,
                altitude,
                null,
                bearing,
                null);

        Track.Id trackId = new Track.Id(cursor.getLong(idx.trackIdIndex));
        Marker marker = new Marker(trackId, position);

        if (!cursor.isNull(idx.idIndex)) {
            marker.setId(new Marker.Id(cursor.getLong(idx.idIndex)));
        }
        if (!cursor.isNull(idx.nameIndex)) {
            marker.setName(cursor.getString(idx.nameIndex));
        }
        if (!cursor.isNull(idx.descriptionIndex)) {
            marker.setDescription(cursor.getString(idx.descriptionIndex));
        }
        if (!cursor.isNull(idx.categoryIndex)) {
            marker.setCategory(cursor.getString(idx.categoryIndex));
        }
        if (!cursor.isNull(idx.iconIndex)) {
            marker.setIcon(cursor.getString(idx.iconIndex));
        }
        if (!cursor.isNull(idx.photoUrlIndex)) {
            String photoUrl = cursor.getString(idx.photoUrlIndex);
            if (photoUrl.isEmpty()) {
                // Before v4.18.0: a marker without a picture as URL ""
                // TODO Data should be migrated.
                marker.setPhotoUrl(null);
            } else {
                marker.setPhotoUrl(Uri.parse(cursor.getString(idx.photoUrlIndex)));
            }
        }


        return marker;
    }

    public void deleteMarker(Context context, Marker.Id markerId) {
        final Marker marker = getMarker(markerId);
        deleteMarkerPhoto(context, marker);
        contentResolver.delete(MarkerColumns.CONTENT_URI, DbUtils.eqClause(MarkerColumns._ID), DbUtils.idArgs(markerId.id()));
    }

    /**
     * @return null if not able to get the next marker number.
     */
    public Integer getNextMarkerNumber(@NonNull Track.Id trackId) {
        String[] projection = {MarkerColumns._ID};
        String selection = DbUtils.eqClause(MarkerColumns.TRACKID);
        String[] selectionArgs = DbUtils.idArgs(trackId.id());
        try (Cursor cursor = getMarkerCursor(projection, selection, selectionArgs, MarkerColumns._ID, -1)) {
            if (cursor != null) {
                return cursor.getCount();
            }
        }
        return null;
    }

    public Marker getMarker(@NonNull Marker.Id markerId) {
        try (Cursor cursor = getMarkerCursor(null, DbUtils.eqClause(MarkerColumns._ID), DbUtils.idArgs(markerId.id()), MarkerColumns._ID, 1)) {
            if (cursor != null && cursor.moveToFirst()) {
                return createMarker(cursor);
            }
        }
        return null;
    }

    /**
     * The caller owns the returned cursor and is responsible for closing it.
     *
     * @param trackId     the track id
     * @param minMarkerId the minimum marker id. null to ignore
     * @param maxCount    the maximum number of markers to return. -1 for no limit
     */
    public Cursor getMarkerCursor(@NonNull Track.Id trackId, @Nullable Marker.Id minMarkerId, int maxCount) {
        String selection;
        String[] selectionArgs;
        if (minMarkerId != null) {
            selection = DbUtils.eqClause(MarkerColumns.TRACKID) + " AND " + MarkerColumns._ID + ">=?";
            selectionArgs = new String[]{DbUtils.idArgs(trackId.id())[0], DbUtils.idArgs(minMarkerId.id())[0]};
        } else {
            selection = DbUtils.eqClause(MarkerColumns.TRACKID);
            selectionArgs = DbUtils.idArgs(trackId.id());
        }
        return getMarkerCursor(null, selection, selectionArgs, MarkerColumns._ID, maxCount);
    }

    @Deprecated //TODO Move to test package
    @VisibleForTesting
    public List<Marker> getMarkers(Track.Id trackId) {
        return DbUtils.cursorToList(getMarkerCursor(trackId, null, -1), this::createMarker);
    }

    // TODO Merge with updateMarker
    public Marker.Id insertMarker(@NonNull Marker marker) {
        marker.setId(null);
        Uri uri = contentResolver.insert(MarkerColumns.CONTENT_URI, createContentValues(marker));
        return new Marker.Id(ContentUris.parseId(uri));
    }

    private void deleteMarkerPhoto(Context context, Marker marker) {
        if (marker != null && marker.hasPhoto()) {
            Uri uri = marker.getPhotoUrl();
            File file = MarkerUtils.buildInternalPhotoFile(context, marker.getTrackId(), uri);
            if (file.exists()) {
                File parent = file.getParentFile();
                file.delete();
                if (parent.listFiles().length == 0) {
                    parent.delete();
                }
            }
        }
    }

    /**
     * @param updateMarker the marker with updated data.
     * @return true if successful.
     */
    public boolean updateMarker(Context context, Marker updateMarker) {
        Marker savedMarker = getMarker(updateMarker.getId());
        if (!updateMarker.hasPhoto()) {
            deleteMarkerPhoto(context, savedMarker);
        }
        int rows = contentResolver.update(MarkerColumns.CONTENT_URI, createContentValues(updateMarker), DbUtils.eqClause(MarkerColumns._ID), DbUtils.idArgs(updateMarker.getId().id()));
        return rows == 1;
    }

    ContentValues createContentValues(@NonNull Marker marker) {
        ContentValues values = new ContentValues();

        if (marker.getId() != null) {
            values.put(MarkerColumns._ID, marker.getId().id());
        }
        values.put(MarkerColumns.NAME, marker.getName());
        values.put(MarkerColumns.DESCRIPTION, marker.getDescription());
        values.put(MarkerColumns.CATEGORY, marker.getCategory());
        values.put(MarkerColumns.ICON, marker.getIcon());
        values.put(MarkerColumns.TRACKID, marker.getTrackId().id());
        values.put(MarkerColumns.LONGITUDE, (int) (marker.getPosition().longitude() * 1E6));
        values.put(MarkerColumns.LATITUDE, (int) (marker.getPosition().latitude() * 1E6));
        values.put(MarkerColumns.TIME, marker.getTime().toEpochMilli());
        if (marker.hasAltitude()) {
            values.put(MarkerColumns.ALTITUDE, marker.getAltitude().toM());
        }
        if (marker.hasAccuracy()) {
            values.put(MarkerColumns.ACCURACY, marker.getAccuracy().toM());
        }
        if (marker.hasBearing()) {
            values.put(MarkerColumns.BEARING, marker.getBearing());
        }

        if (marker.hasPhoto()) {
            values.put(MarkerColumns.PHOTOURL, marker.getPhotoUrl().toString());
        }
        return values;
    }

    /**
     * @param projection    the projection
     * @param selection     the selection
     * @param selectionArgs the selection args
     * @param sortOrder     the sort order
     * @param maxCount      the maximum number of markers
     */
    private Cursor getMarkerCursor(String[] projection, String selection, String[] selectionArgs, String sortOrder, int maxCount) {
        if (sortOrder == null) {
            sortOrder = MarkerColumns._ID;
        }
        if (maxCount >= 0) {
            sortOrder += " LIMIT " + maxCount;
        }
        return contentResolver.query(MarkerColumns.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
    }

    public List<Marker> searchMarkers(Track.Id trackId, String query) {
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;

        if (query == null) {
            if (trackId != null) {
                selection = DbUtils.eqClause(MarkerColumns.TRACKID);
                selectionArgs = DbUtils.idArgs(trackId.id());
            }
        } else {
            selection = MarkerColumns.NAME + " LIKE ? OR " +
                    MarkerColumns.DESCRIPTION + " LIKE ? OR " +
                    MarkerColumns.CATEGORY + " LIKE ?";
            selectionArgs = new String[]{"%" + query + "%", "%" + query + "%", "%" + query + "%"};
            sortOrder = MarkerColumns.DEFAULT_SORT_ORDER + " DESC";
        }

        return DbUtils.cursorToList(getMarkerCursor(null, selection, selectionArgs, sortOrder, -1), this::createMarker);
    }

    /**
     * Fills a {@link TrackPoint} from a cursor.
     *
     * @param cursor  the cursor pointing to a trackPoint.
     * @param indexes the cached trackPoints indexes
     */
    static TrackPoint fillTrackPoint(Cursor cursor, CachedTrackPointsIndexes indexes) {
        TrackPoint trackPoint = new TrackPoint(
                new TrackPoint.Id(cursor.getInt(indexes.idIndex)),
                TrackPoint.Type.getById(cursor.getInt(indexes.typeIndex)),
                new Position(
                        Instant.ofEpochMilli(cursor.getLong(indexes.timeIndex)),
                        !cursor.isNull(indexes.latitudeIndex) ? ((double) cursor.getInt(indexes.latitudeIndex)) / 1E6 : null,
                        !cursor.isNull(indexes.longitudeIndex) ? ((double) cursor.getInt(indexes.longitudeIndex)) / 1E6 : null,
                        !cursor.isNull(indexes.accuracyIndex) ? Distance.of(cursor.getFloat(indexes.accuracyIndex)) : null,
                        !cursor.isNull(indexes.altitudeIndex) ? Altitude.WGS84.of(cursor.getFloat(indexes.altitudeIndex)) : null,
                        !cursor.isNull(indexes.accuracyVerticalIndex) ? Distance.of(cursor.getFloat(indexes.accuracyVerticalIndex)) : null,
                        !cursor.isNull(indexes.bearingIndex) ? cursor.getFloat(indexes.bearingIndex) : null,
                        !cursor.isNull(indexes.speedIndex) ? Speed.of(cursor.getFloat(indexes.speedIndex)) : null
                ));

        if (!cursor.isNull(indexes.sensorHeartRateIndex)) {
            trackPoint.setHeartRate(cursor.getFloat(indexes.sensorHeartRateIndex));
        }
        if (!cursor.isNull(indexes.sensorCadenceIndex)) {
            trackPoint.setCadence(cursor.getFloat(indexes.sensorCadenceIndex));
        }
        if (!cursor.isNull(indexes.sensorDistanceIndex)) {
            trackPoint.setSensorDistance(Distance.of(cursor.getFloat(indexes.sensorDistanceIndex)));
        }
        if (!cursor.isNull(indexes.sensorPowerIndex)) {
            trackPoint.setPower(cursor.getFloat(indexes.sensorPowerIndex));
        }

        if (!cursor.isNull(indexes.altitudeGainIndex)) {
            trackPoint.setAltitudeGain(cursor.getFloat(indexes.altitudeGainIndex));
        }
        if (!cursor.isNull(indexes.altitudeLossIndex)) {
            trackPoint.setAltitudeLoss(cursor.getFloat(indexes.altitudeLossIndex));
        }

        return trackPoint;
    }

    //TODO Only used for file import; might be better to replace it.
    //TODO Rename to bulkInsert
    public int bulkInsertTrackPoint(List<TrackPoint> trackPoints, Track.Id trackId) {
        ContentValues[] values = new ContentValues[trackPoints.size()];
        for (int i = 0; i < trackPoints.size(); i++) {
            values[i] = createContentValues(trackPoints.get(i), trackId);
        }
        return contentResolver.bulkInsert(TrackPointsColumns.CONTENT_URI_BY_ID, values);
    }

    //TODO Set trackId in this method.
    public int bulkInsertMarkers(List<Marker> markers, Track.Id trackId) {
        ContentValues[] values = new ContentValues[markers.size()];
        for (int i = 0; i < markers.size(); i++) {
            values[i] = createContentValues(markers.get(i));
        }
        return contentResolver.bulkInsert(MarkerColumns.CONTENT_URI, values);
    }

    /**
     * Gets the last location id for a track.
     * Returns -1L if it doesn't exist.
     *
     * @param trackId the track id
     */
    @Deprecated
    public TrackPoint.Id getLastTrackPointId(@NonNull Track.Id trackId) {
        String selection = TrackPointsColumns._ID + "=(SELECT MAX(" + TrackPointsColumns._ID + ") from " + TrackPointsColumns.TABLE_NAME + " WHERE " + DbUtils.eqClause(TrackPointsColumns.TRACKID) + ")";
        String[] selectionArgs = DbUtils.idArgs(trackId.id());
        try (Cursor cursor = getTrackPointCursor(new String[]{TrackPointsColumns._ID}, selection, selectionArgs, TrackPointsColumns._ID)) {
            if (cursor != null && cursor.moveToFirst()) {
                return new TrackPoint.Id(cursor.getLong(cursor.getColumnIndexOrThrow(TrackPointsColumns._ID)));
            }
        }
        return null;
    }

    /**
     * Gets the trackPoint id for a location.
     */
    @Deprecated
    public TrackPoint.Id getTrackPointId(Track.Id trackId, Position position) {
        String selection = TrackPointsColumns._ID + "=(SELECT MAX(" + TrackPointsColumns._ID + ") FROM " + TrackPointsColumns.TABLE_NAME + " WHERE " + DbUtils.eqClause(TrackPointsColumns.TRACKID) + " AND " + TrackPointsColumns.TIME + "=?)";
        String[] selectionArgs = new String[]{DbUtils.idArgs(trackId.id())[0], Long.toString(position.time().toEpochMilli())};
        try (Cursor cursor = getTrackPointCursor(new String[]{TrackPointsColumns._ID}, selection, selectionArgs, TrackPointsColumns._ID)) {
            if (cursor != null && cursor.moveToFirst()) {
                return new TrackPoint.Id(cursor.getLong(cursor.getColumnIndexOrThrow(TrackPointsColumns._ID)));
            }
        }
        return null;
    }

    /**
     * Creates a {@link TrackPoint} object from a cursor.
     *
     * @param cursor the cursor pointing to the location
     */
    public TrackPoint createTrackPoint(Cursor cursor) {
        return fillTrackPoint(cursor, new CachedTrackPointsIndexes(cursor));
    }

    /**
     * Creates a location cursor. The caller owns the returned cursor and is responsible for closing it.
     *
     * @param trackId           the track id
     * @param startTrackPointId the starting trackPoint id. `null` to ignore
     */
    @NonNull
    public Cursor getTrackPointCursor(@NonNull Track.Id trackId, TrackPoint.Id startTrackPointId) {
        String selection;
        String[] selectionArgs;
        if (startTrackPointId != null) {
            selection = DbUtils.eqClause(TrackPointsColumns.TRACKID) + " AND " + TrackPointsColumns._ID + ">=?";
            selectionArgs = new String[]{DbUtils.idArgs(trackId.id())[0], DbUtils.idArgs(startTrackPointId.id())[0]};
        } else {
            selection = DbUtils.eqClause(TrackPointsColumns.TRACKID);
            selectionArgs = DbUtils.idArgs(trackId.id());
        }

        return getTrackPointCursor(null, selection, selectionArgs, TrackPointsColumns.DEFAULT_SORT_ORDER);
    }

    /**
     * Gets the last valid location for a track.
     * Returns null if it doesn't exist.
     *
     * @param trackId the track id
     */
    @Deprecated
    public TrackPoint getLastValidTrackPoint(Track.Id trackId) {
        String selection = TrackPointsColumns._ID + "=(SELECT MAX(" + TrackPointsColumns._ID + ") FROM " + TrackPointsColumns.TABLE_NAME + " WHERE " + DbUtils.eqClause(TrackPointsColumns.TRACKID) + " AND " + TrackPointsColumns.TYPE + " IN (" + TrackPoint.Type.SEGMENT_START_AUTOMATIC.type_db + "," + TrackPoint.Type.TRACKPOINT.type_db + "))";
        String[] selectionArgs = DbUtils.idArgs(trackId.id());
        return findTrackPointBy(selection, selectionArgs);
    }

    /**
     * Inserts a trackPoint.
     *
     * @param trackPoint the trackPoint
     * @param trackId    the track id
     * @return the content provider URI of the inserted trackPoint
     */
    public Uri insertTrackPoint(TrackPoint trackPoint, Track.Id trackId) {
        return contentResolver.insert(TrackPointsColumns.CONTENT_URI_BY_ID, createContentValues(trackPoint, trackId));
    }

    /**
     * Creates the {@link ContentValues} for a {@link TrackPoint}.
     *
     * @param trackPoint the trackPoint
     * @param trackId    the track id
     */
    private ContentValues createContentValues(TrackPoint trackPoint, Track.Id trackId) {
        ContentValues values = new ContentValues();
        values.put(TrackPointsColumns.TRACKID, trackId.id());
        values.put(TrackPointsColumns.TYPE, trackPoint.getType().type_db);

        if (trackPoint.hasLocation()) {
            values.put(TrackPointsColumns.LATITUDE, (int) (trackPoint.getPosition().latitude() * 1E6));
            values.put(TrackPointsColumns.LONGITUDE, (int) (trackPoint.getPosition().longitude() * 1E6));
        }
        values.put(TrackPointsColumns.TIME, trackPoint.getTime().toEpochMilli());
        if (trackPoint.hasAltitude()) {
            values.put(TrackPointsColumns.ALTITUDE, trackPoint.getAltitude().toM());
        }
        if (trackPoint.hasHorizontalAccuracy()) {
            values.put(TrackPointsColumns.HORIZONTAL_ACCURACY, trackPoint.getHorizontalAccuracy().toM());
        }
        if (trackPoint.hasSpeed()) {
            values.put(TrackPointsColumns.SPEED, trackPoint.getSpeed().toMPS());
        }
        if (trackPoint.hasBearing()) {
            values.put(TrackPointsColumns.BEARING, trackPoint.getBearing());
        }

        if (trackPoint.hasHeartRate()) {
            values.put(TrackPointsColumns.SENSOR_HEARTRATE, trackPoint.getHeartRate().getBPM());
        }
        if (trackPoint.hasCadence()) {
            values.put(TrackPointsColumns.SENSOR_CADENCE, trackPoint.getCadence().getRPM());
        }
        if (trackPoint.hasSensorDistance()) {
            values.put(TrackPointsColumns.SENSOR_DISTANCE, trackPoint.getSensorDistance().toM());
        }
        if (trackPoint.hasPower()) {
            values.put(TrackPointsColumns.SENSOR_POWER, trackPoint.getPower().getW());
        }

        if (trackPoint.hasAltitudeGain()) {
            values.put(TrackPointsColumns.ALTITUDE_GAIN, trackPoint.getAltitudeGain());
        }
        if (trackPoint.hasAltitudeLoss()) {
            values.put(TrackPointsColumns.ALTITUDE_LOSS, trackPoint.getAltitudeLoss());
        }

        return values;
    }

    /**
     * Creates a new read-only iterator over a given track's points.
     * It provides a lightweight way of iterating over long tracks without failing due to the underlying cursor limitations.
     * Since it's a read-only iterator, {@link Iterator#remove()} always throws {@link UnsupportedOperationException}.
     * Each call to {@link TrackPointIterator#next()} may advance to the next DB record.
     * When done with iteration, {@link TrackPointIterator#close()} must be called.
     *
     * @param trackId           the track id
     * @param startTrackPointId the starting trackPoint id. `null` to ignore
     */
    public TrackPointIterator getTrackPointLocationIterator(final Track.Id trackId, final TrackPoint.Id startTrackPointId) {
        return new TrackPointIterator(this, trackId, startTrackPointId);
    }

    @Deprecated
    private TrackPoint findTrackPointBy(String selection, String[] selectionArgs) {
        try (Cursor cursor = getTrackPointCursor(null, selection, selectionArgs, TrackPointsColumns._ID)) {
            if (cursor != null && cursor.moveToNext()) {
                return createTrackPoint(cursor);
            }
        }
        return null;
    }

    /**
     * Gets a trackPoint cursor.
     *
     * @param projection    the projection
     * @param selection     the selection
     * @param selectionArgs the selection arguments
     * @param sortOrder     the sort order
     */
    private Cursor getTrackPointCursor(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return contentResolver.query(TrackPointsColumns.CONTENT_URI_BY_ID, projection, selection, selectionArgs, sortOrder);
    }

    public static String formatIdListForUri(Track.Id... trackIds) {
        long[] ids = new long[trackIds.length];
        for (int i = 0; i < trackIds.length; i++) {
            ids[i] = trackIds[i].id();
        }

        return formatIdListForUri(ids);
    }

    /**
     * Formats an array of IDs as comma separated string value
     *
     * @param ids array with IDs
     * @return comma separated list of ids
     */
    private static String formatIdListForUri(long[] ids) {
        StringBuilder idsPathSegment = new StringBuilder();
        for (long id : ids) {
            if (idsPathSegment.length() > 0) {
                idsPathSegment.append(ID_SEPARATOR);
            }
            idsPathSegment.append(id);
        }
        return idsPathSegment.toString();
    }

    public static String[] parseTrackIdsFromUri(Uri url) {
        return TextUtils.split(url.getLastPathSegment(), ID_SEPARATOR);
    }

    public SensorStatistics getSensorStats(@NonNull Track.Id trackId) {
        SensorStatistics sensorStatistics = null;
        try (Cursor cursor = contentResolver.query(ContentUris.withAppendedId(TracksColumns.CONTENT_URI_SENSOR_STATS, trackId.id()), null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int MAX_HR_INDEX = cursor.getColumnIndexOrThrow(TrackPointsColumns.ALIAS_MAX_HR);
                final int AVG_HR_INDEX = cursor.getColumnIndexOrThrow(TrackPointsColumns.ALIAS_AVG_HR);
                final int MAX_CADENCE_INDEX = cursor.getColumnIndexOrThrow(TrackPointsColumns.ALIAS_MAX_CADENCE);
                final int AVG_CADENCE_INDEX = cursor.getColumnIndexOrThrow(TrackPointsColumns.ALIAS_AVG_CADENCE);
                final int MAX_POWER_INDEX = cursor.getColumnIndexOrThrow(TrackPointsColumns.ALIAS_MAX_POWER);
                final int AVG_POWER_INDEX = cursor.getColumnIndexOrThrow(TrackPointsColumns.ALIAS_AVG_POWER);
                sensorStatistics = new SensorStatistics(
                        !cursor.isNull(MAX_HR_INDEX) ? HeartRate.of(cursor.getFloat(MAX_HR_INDEX)) : null,
                        !cursor.isNull(AVG_HR_INDEX) ? HeartRate.of(cursor.getFloat(AVG_HR_INDEX)) : null,
                        !cursor.isNull(MAX_CADENCE_INDEX) ? Cadence.of(cursor.getFloat(MAX_CADENCE_INDEX)) : null,
                        !cursor.isNull(AVG_CADENCE_INDEX) ? Cadence.of(cursor.getFloat(AVG_CADENCE_INDEX)) : null,
                        !cursor.isNull(MAX_POWER_INDEX) ? Power.of(cursor.getFloat(MAX_POWER_INDEX)) : null,
                        !cursor.isNull(AVG_POWER_INDEX) ? Power.of(cursor.getFloat(AVG_POWER_INDEX)) : null
                );
            }

        }
        return sensorStatistics;
    }
}
