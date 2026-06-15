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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Arrays;

import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.data.tables.MarkerColumns;
import de.dennisguse.opentracks.data.tables.TrackPointsColumns;
import de.dennisguse.opentracks.data.tables.TracksColumns;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/**
 * A {@link ContentProvider} that handles access to track points, tracks, and markers tables.
 * <p>
 * Data consistency is enforced using Foreign Key Constraints within the database incl. cascading deletes.
 *
 * @author Leif Hendrik Wilden
 */
public class CustomContentProvider extends ContentProvider {

    private static final String TAG = CustomContentProvider.class.getSimpleName();

    private static final String SQL_LIST_DELIMITER = ",";

    private static final int TOTAL_DELETED_ROWS_VACUUM_THRESHOLD = 10000;

    private final UriMatcher uriMatcher;

    private SQLiteDatabase db;

    /**
     * The string representing the query that compute sensor stats from trackpoints table.
     * It computes the average for heart rate, cadence and power (duration-based average) and the maximum for heart rate, cadence and power.
     * Finally, it ignores manual pause (SEGMENT_START_MANUAL).
     */
    private final String SENSOR_STATS_QUERY =
            "WITH time_select as " +
                "(SELECT t1." + TrackPointsColumns.TIME + " * (t1." + TrackPointsColumns.TYPE + " NOT IN (" + TrackPoint.Type.SEGMENT_START_MANUAL.type_db + ")) time_value " +
                "FROM " + TrackPointsColumns.TABLE_NAME + " t1 " +
                "WHERE t1." + TrackPointsColumns._ID + " > t." + TrackPointsColumns._ID + " AND t1." + TrackPointsColumns.TRACKID + " = ? ORDER BY _id LIMIT 1) " +

            "SELECT " +
                "SUM(t." + TrackPointsColumns.SENSOR_HEARTRATE + " * (COALESCE(MAX(t." + TrackPointsColumns.TIME + ", (SELECT time_value FROM time_select)), t." + TrackPointsColumns.TIME + ") - t." + TrackPointsColumns.TIME + ")) " +
                "/ " +
                "SUM(COALESCE(MAX(t." + TrackPointsColumns.TIME + ", (SELECT time_value FROM time_select)), t." + TrackPointsColumns.TIME + ") - t." + TrackPointsColumns.TIME + ") " + TrackPointsColumns.ALIAS_AVG_HR + ", " +

                "MAX(t." + TrackPointsColumns.SENSOR_HEARTRATE + ") " + TrackPointsColumns.ALIAS_MAX_HR + ", " +

                "SUM(t." + TrackPointsColumns.SENSOR_CADENCE + " * (COALESCE(MAX(t." + TrackPointsColumns.TIME + ", (SELECT time_value FROM time_select)), t." + TrackPointsColumns.TIME + ") - t." + TrackPointsColumns.TIME + ")) " +
                "/ " +
                "SUM(COALESCE(MAX(t." + TrackPointsColumns.TIME + ", (SELECT time_value FROM time_select)), t." + TrackPointsColumns.TIME + ") - t." + TrackPointsColumns.TIME + ") " + TrackPointsColumns.ALIAS_AVG_CADENCE + ", " +

                "MAX(t." + TrackPointsColumns.SENSOR_CADENCE + ") " + TrackPointsColumns.ALIAS_MAX_CADENCE + ", " +

                "SUM(t." + TrackPointsColumns.SENSOR_POWER + " * (COALESCE(MAX(t." + TrackPointsColumns.TIME + ", (SELECT time_value FROM time_select)), t." + TrackPointsColumns.TIME + ") - t." + TrackPointsColumns.TIME + ")) " +
                "/ " +
                "SUM(COALESCE(MAX(t." + TrackPointsColumns.TIME + ", (SELECT time_value FROM time_select)), t." + TrackPointsColumns.TIME + ") - t." + TrackPointsColumns.TIME + ") " + TrackPointsColumns.ALIAS_AVG_POWER + ", " +

                "MAX(t." + TrackPointsColumns.SENSOR_POWER + ") " + TrackPointsColumns.ALIAS_MAX_POWER + " " +

            "FROM " + TrackPointsColumns.TABLE_NAME + " t " +
            "WHERE t." + TrackPointsColumns.TRACKID + " = ? " +
            "AND t." + TrackPointsColumns.TYPE + " NOT IN (" + TrackPoint.Type.SEGMENT_START_MANUAL.type_db + ")";

    public CustomContentProvider() {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, TrackPointsColumns.CONTENT_URI_BY_ID.getPath(), UrlType.TRACKPOINTS.matchCode);
        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, TrackPointsColumns.CONTENT_URI_BY_ID.getPath() + "/#", UrlType.TRACKPOINTS_BY_ID.matchCode);
        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, TrackPointsColumns.CONTENT_URI_BY_TRACKID.getPath() + "/*", UrlType.TRACKPOINTS_BY_TRACKID.matchCode);

        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, TracksColumns.CONTENT_URI.getPath(), UrlType.TRACKS.matchCode);
        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, TracksColumns.CONTENT_URI_SENSOR_STATS.getPath() + "/#", UrlType.TRACKS_SENSOR_STATS.matchCode);
        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, TracksColumns.CONTENT_URI.getPath() + "/*", UrlType.TRACKS_BY_ID.matchCode);

        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, MarkerColumns.CONTENT_URI.getPath(), UrlType.MARKERS.matchCode);
        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, MarkerColumns.CONTENT_URI.getPath() + "/#", UrlType.MARKERS_BY_ID.matchCode);
        uriMatcher.addURI(ContentProviderUtils.AUTHORITY_PACKAGE, MarkerColumns.CONTENT_URI_BY_TRACKID.getPath() + "/*", UrlType.MARKERS_BY_TRACKID.matchCode);
    }

    @Override
    public boolean onCreate() {
        return onCreate(getContext());
    }

    /**
     * Helper method to make onCreate is testable.
     *
     * @param context context to creates database
     * @return true means run successfully
     */
    @VisibleForTesting
    boolean onCreate(Context context) {
        CustomSQLiteOpenHelper databaseHelper = new CustomSQLiteOpenHelper(context);
        try {
            db = databaseHelper.getWritableDatabase();
            // Necessary to enable cascade deletion from Track to TrackPoints and Markers
            db.setForeignKeyConstraintsEnabled(true);
        } catch (SQLiteException e) {
            Log.e(TAG, "Unable to open database for writing.", e);
        }
        return db != null;
    }

    @Override
    public int delete(@NonNull Uri url, String where, String[] selectionArgs) {
        String table = switch (getUrlType(url)) {
            case TRACKPOINTS -> TrackPointsColumns.TABLE_NAME;
            case TRACKS -> TracksColumns.TABLE_NAME;
            case MARKERS -> MarkerColumns.TABLE_NAME;
            default -> throw new IllegalArgumentException("Unknown URL " + url);
        };

        Log.w(TAG, "Deleting from table " + table);
        int totalChangesBefore = getTotalChanges();
        int deletedRowsFromTable = DbUtils.runInTransaction(db, () -> {
            int rows = db.delete(table, where, selectionArgs);
            Log.i(TAG, "Deleted " + rows + " rows of table " + table);
            return rows;
        });
        getContext().getContentResolver().notifyChange(url, null, false);

        int totalChanges = getTotalChanges() - totalChangesBefore;
        Log.i(TAG, "Deleted " + totalChanges + " total rows from database");

        PreferencesUtils.addTotalRowsDeleted(totalChanges);
        int totalRowsDeleted = PreferencesUtils.getTotalRowsDeleted();
        if (totalRowsDeleted > TOTAL_DELETED_ROWS_VACUUM_THRESHOLD) {
            Log.i(TAG, "TotalRowsDeleted " + totalRowsDeleted + ", starting to vacuum the database.");
            db.execSQL("VACUUM");
            PreferencesUtils.resetTotalRowsDeleted();
        }

        return deletedRowsFromTable;
    }

    private int getTotalChanges() {
        int totalCount;
        try (Cursor cursor = db.rawQuery("SELECT total_changes()", null)) {
            cursor.moveToNext();
            totalCount = cursor.getInt(0);
        }
        return totalCount;
    }

    @Override
    public String getType(@NonNull Uri url) {
        return switch (getUrlType(url)) {
            case TRACKPOINTS -> TrackPointsColumns.CONTENT_TYPE;
            case TRACKPOINTS_BY_ID, TRACKPOINTS_BY_TRACKID -> TrackPointsColumns.CONTENT_ITEMTYPE;
            case TRACKS -> TracksColumns.CONTENT_TYPE;
            case TRACKS_BY_ID -> TracksColumns.CONTENT_ITEMTYPE;
            case MARKERS -> MarkerColumns.CONTENT_TYPE;
            case MARKERS_BY_ID, MARKERS_BY_TRACKID -> MarkerColumns.CONTENT_ITEMTYPE;
            default -> throw new IllegalArgumentException("Unknown URL " + url);
        };
    }

    @Override
    public Uri insert(@NonNull Uri url, ContentValues initialValues) {
        if (initialValues == null) {
            initialValues = new ContentValues();
        }
        Uri result = DbUtils.runInTransaction(db, () ->
                insertContentValues(url, getUrlType(url), initialValues));
        getContext().getContentResolver().notifyChange(url, null, false);
        return result;
    }

    @Override
    public int bulkInsert(@NonNull Uri url, @NonNull ContentValues[] valuesBulk) {
        int numInserted = DbUtils.runInTransaction(db, () -> {
            // Use a transaction in order to make the insertions run as a single batch
            UrlType urlType = getUrlType(url);
            int count;
            for (count = 0; count < valuesBulk.length; count++) {
                ContentValues contentValues = valuesBulk[count];
                if (contentValues == null) {
                    contentValues = new ContentValues();
                }
                insertContentValues(url, urlType, contentValues);
            }
            return count;
        });
        getContext().getContentResolver().notifyChange(url, null, false);
        return numInserted;
    }

    @Override
    public Cursor query(@NonNull Uri url, String[] projection, String selection, String[] selectionArgs, String sort) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        String sortOrder = null;
        switch (getUrlType(url)) {
            case TRACKPOINTS -> {
                queryBuilder.setTables(TrackPointsColumns.TABLE_NAME);
                sortOrder = sort != null ? sort : TrackPointsColumns.DEFAULT_SORT_ORDER;
            }
            case TRACKPOINTS_BY_ID -> {
                queryBuilder.setTables(TrackPointsColumns.TABLE_NAME);
                queryBuilder.appendWhere(TrackPointsColumns._ID + "=" + ContentUris.parseId(url));
            }
            case TRACKPOINTS_BY_TRACKID -> {
                queryBuilder.setTables(TrackPointsColumns.TABLE_NAME);
                queryBuilder.appendWhere(TrackPointsColumns.TRACKID + " IN (" + TextUtils.join(SQL_LIST_DELIMITER, ContentProviderUtils.parseTrackIdsFromUri(url)) + ")");
            }
            case TRACKS -> {
                if (projection != null && Arrays.asList(projection).contains(TracksColumns.MARKER_COUNT)) {
                    queryBuilder.setTables(TracksColumns.TABLE_NAME + " LEFT OUTER JOIN (SELECT " + MarkerColumns.TRACKID + " AS markerTrackId, COUNT(*) AS " + TracksColumns.MARKER_COUNT + " FROM " + MarkerColumns.TABLE_NAME + " GROUP BY " + MarkerColumns.TRACKID + ") ON (" + TracksColumns.TABLE_NAME + "." + TracksColumns._ID + "= markerTrackId)");
                } else {
                    queryBuilder.setTables(TracksColumns.TABLE_NAME);
                }
                sortOrder = sort != null ? sort : TracksColumns.DEFAULT_SORT_ORDER;
            }
            case TRACKS_BY_ID -> {
                queryBuilder.setTables(TracksColumns.TABLE_NAME);
                queryBuilder.appendWhere(TracksColumns._ID + " IN (" + TextUtils.join(SQL_LIST_DELIMITER, ContentProviderUtils.parseTrackIdsFromUri(url)) + ")");
            }
            case TRACKS_SENSOR_STATS -> {
                long trackId = ContentUris.parseId(url);
                return db.rawQuery(SENSOR_STATS_QUERY, new String[]{String.valueOf(trackId), String.valueOf(trackId)});
            }
            case MARKERS -> {
                queryBuilder.setTables(MarkerColumns.TABLE_NAME);
                sortOrder = sort != null ? sort : MarkerColumns.DEFAULT_SORT_ORDER;
            }
            case MARKERS_BY_ID -> {
                queryBuilder.setTables(MarkerColumns.TABLE_NAME);
                queryBuilder.appendWhere(MarkerColumns._ID + "=" + ContentUris.parseId(url));
            }
            case MARKERS_BY_TRACKID -> {
                queryBuilder.setTables(MarkerColumns.TABLE_NAME);
                queryBuilder.appendWhere(MarkerColumns.TRACKID + " IN (" + TextUtils.join(SQL_LIST_DELIMITER, ContentProviderUtils.parseTrackIdsFromUri(url)) + ")");
            }
            default -> throw new IllegalArgumentException("Unknown url " + url);
        }
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), url);
        return cursor;
    }

    @Override
    public int update(@NonNull Uri url, ContentValues values, String where, String[] selectionArgs) {
        String table;
        String whereClause;
        switch (getUrlType(url)) {
            case TRACKPOINTS -> {
                table = TrackPointsColumns.TABLE_NAME;
                whereClause = where;
            }
            case TRACKPOINTS_BY_ID -> {
                table = TrackPointsColumns.TABLE_NAME;
                whereClause = DbUtils.buildWhereById(TrackPointsColumns._ID, ContentUris.parseId(url), where);
            }
            case TRACKS -> {
                table = TracksColumns.TABLE_NAME;
                whereClause = where;
            }
            case TRACKS_BY_ID -> {
                table = TracksColumns.TABLE_NAME;
                whereClause = DbUtils.buildWhereById(TracksColumns._ID, ContentUris.parseId(url), where);
            }
            case MARKERS -> {
                table = MarkerColumns.TABLE_NAME;
                whereClause = where;
            }
            case MARKERS_BY_ID -> {
                table = MarkerColumns.TABLE_NAME;
                whereClause = DbUtils.buildWhereById(MarkerColumns._ID, ContentUris.parseId(url), where);
            }
            default -> throw new IllegalArgumentException("Unknown url " + url);
        }
        final String finalTable = table;
        final String finalWhereClause = whereClause;
        int count = DbUtils.runInTransaction(db, () ->
                db.update(finalTable, values, finalWhereClause, selectionArgs));
        getContext().getContentResolver().notifyChange(url, null, false);
        return count;
    }

    @NonNull
    private UrlType getUrlType(Uri url) {
        return UrlType.fromMatchCode(uriMatcher.match(url));
    }

    /**
     * Inserts a content based on the url type.
     *
     * @param url           the content url
     * @param urlType       the url type
     * @param contentValues the content values
     */
    private Uri insertContentValues(Uri url, UrlType urlType, ContentValues contentValues) {
        return switch (urlType) {
            case TRACKPOINTS -> {
                if (!contentValues.containsKey(TrackPointsColumns.TIME)) {
                    throw new IllegalArgumentException("Latitude, longitude, and time values are required.");
                }
                yield insertRow(TrackPointsColumns.TABLE_NAME, contentValues, TrackPointsColumns.CONTENT_URI_BY_ID, url);
            }
            case TRACKS -> insertRow(TracksColumns.TABLE_NAME, contentValues, TracksColumns.CONTENT_URI, url);
            case MARKERS -> insertRow(MarkerColumns.TABLE_NAME, contentValues, MarkerColumns.CONTENT_URI, url);
            default -> throw new IllegalArgumentException("Unknown url " + url);
        };
    }

    private Uri insertRow(String table, ContentValues values, Uri baseUri, Uri url) {
        long rowId = db.insert(table, table, values);
        if (rowId >= 0) {
            return ContentUris.appendId(baseUri.buildUpon(), rowId).build();
        }
        throw new SQLException("Failed to insert into " + table + " " + url);
    }

    @VisibleForTesting
    enum UrlType {
        TRACKPOINTS(0),
        TRACKPOINTS_BY_ID(1),
        TRACKPOINTS_BY_TRACKID(2),
        TRACKS(3),
        TRACKS_SENSOR_STATS(4),
        TRACKS_BY_ID(5),
        MARKERS(6),
        MARKERS_BY_ID(7),
        MARKERS_BY_TRACKID(8);

        final int matchCode;

        UrlType(int matchCode) {
            this.matchCode = matchCode;
        }

        static UrlType fromMatchCode(int matchCode) {
            for (UrlType type : values()) {
                if (type.matchCode == matchCode) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown match code: " + matchCode);
        }
    }
}
