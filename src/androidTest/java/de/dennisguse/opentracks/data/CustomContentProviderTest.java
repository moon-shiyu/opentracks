/*
 * Copyright 2012 Google Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.tables.MarkerColumns;
import de.dennisguse.opentracks.data.tables.TrackPointsColumns;
import de.dennisguse.opentracks.data.tables.TracksColumns;

/**
 * Tests {@link CustomContentProvider}.
 *
 * @author Youtao Liu
 */
public class CustomContentProviderTest {

    private CustomContentProvider customContentProvider;
    private final Context context = ApplicationProvider.getApplicationContext();
    private ContentProviderUtils contentProviderUtils;

    @Before
    public void setUp() {
        customContentProvider = new CustomContentProvider() {
        };
        contentProviderUtils = new ContentProviderUtils(context);
        contentProviderUtils.deleteAllTracks(context);
    }

    /**
     * Tests {@link CustomContentProvider#onCreate(android.content.Context)}.
     */
    @Test
    public void testOnCreate() {
        assertTrue(customContentProvider.onCreate(context));
    }

    /**
     * Tests {@link CustomContentProvider#getType(Uri)}.
     */
    @Test
    public void testGetType() {
        assertEquals(TracksColumns.CONTENT_TYPE, customContentProvider.getType(TracksColumns.CONTENT_URI));
        assertEquals(TracksColumns.CONTENT_ITEMTYPE, customContentProvider.getType(ContentUris.appendId(TracksColumns.CONTENT_URI.buildUpon(), 1).build()));

        assertEquals(TrackPointsColumns.CONTENT_TYPE, customContentProvider.getType(TrackPointsColumns.CONTENT_URI_BY_ID));
        assertEquals(TrackPointsColumns.CONTENT_ITEMTYPE, customContentProvider.getType(ContentUris.appendId(TrackPointsColumns.CONTENT_URI_BY_TRACKID.buildUpon(), 1).build()));

        assertEquals(MarkerColumns.CONTENT_TYPE, customContentProvider.getType(MarkerColumns.CONTENT_URI));
        assertEquals(MarkerColumns.CONTENT_ITEMTYPE, customContentProvider.getType(ContentUris.appendId(MarkerColumns.CONTENT_URI.buildUpon(), 1).build()));
    }

    /**
     * Tests querying trackpoints by track id through the installed provider.
     * Covers the {@code trackid IN (...)} clause built for {@code TRACKPOINTS_BY_TRACKID} and the insert helper.
     */
    @Test
    public void testInsertAndQueryTrackPointsByTrackId() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 2);

        Uri uri = ContentUris.appendId(TrackPointsColumns.CONTENT_URI_BY_TRACKID.buildUpon(), trackId.id()).build();
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            assertEquals(2, cursor.getCount());
        }
    }

    /**
     * Tests updating a single track through the {@code tracks/<id>} URI.
     * Covers the per-id WHERE clause building used by {@code TRACKS_BY_ID}.
     */
    @Test
    public void testUpdateTrackById() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        Track track = TestDataUtil.createTrack(trackId);
        track.setName("before");
        contentProviderUtils.insertTrack(track);

        ContentValues values = new ContentValues();
        values.put(TracksColumns.NAME, "after");
        Uri uri = ContentUris.appendId(TracksColumns.CONTENT_URI.buildUpon(), trackId.id()).build();
        int updated = context.getContentResolver().update(uri, values, null, null);

        assertEquals(1, updated);
        assertEquals("after", contentProviderUtils.getTrack(trackId).getName());
    }

    /**
     * Tests that deleting a track row cascades to its trackpoints and markers (FK ON DELETE CASCADE).
     */
    @Test
    public void testDeleteTrackCascades() {
        Track.Id trackId = new Track.Id(System.currentTimeMillis());
        TestDataUtil.createTrackAndInsert(contentProviderUtils, trackId, 2);
        contentProviderUtils.insertMarker(new Marker(trackId, contentProviderUtils.getLastValidTrackPoint(trackId)));

        int deleted = context.getContentResolver().delete(TracksColumns.CONTENT_URI, TracksColumns._ID + "=?", new String[]{String.valueOf(trackId.id())});
        assertEquals(1, deleted);

        Uri trackPointsUri = ContentUris.appendId(TrackPointsColumns.CONTENT_URI_BY_TRACKID.buildUpon(), trackId.id()).build();
        try (Cursor cursor = context.getContentResolver().query(trackPointsUri, null, null, null, null)) {
            assertEquals(0, cursor.getCount());
        }

        Uri markersUri = ContentUris.appendId(MarkerColumns.CONTENT_URI_BY_TRACKID.buildUpon(), trackId.id()).build();
        try (Cursor cursor = context.getContentResolver().query(markersUri, null, null, null, null)) {
            assertEquals(0, cursor.getCount());
        }
    }
}