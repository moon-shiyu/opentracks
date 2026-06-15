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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import de.dennisguse.opentracks.content.data.TestDataUtil;
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
        customContentProvider.onCreate(context);
        contentProviderUtils = new ContentProviderUtils(context);
        contentProviderUtils.deleteAllTracks(context);
    }

    /**
     * Tests {@link CustomContentProvider#onCreate(android.content.Context)}.
     */
    @Test
    public void testOnCreate() {
        CustomContentProvider provider = new CustomContentProvider() {};
        assertTrue(provider.onCreate(context));
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
     * Tests insert and query through the ContentProvider.
     */
    @Test
    public void testInsert_and_query() {
        Track track = TestDataUtil.createTrack(new Track.Id(0));
        Track.Id trackId = contentProviderUtils.insertTrack(track);
        assertNotNull(trackId);

        Track loaded = contentProviderUtils.getTrack(trackId);
        assertNotNull(loaded);
        assertEquals(track.getName(), loaded.getName());
    }

    /**
     * Tests that deleting a track cascades to its trackpoints.
     */
    @Test
    public void testDelete_cascadeTrackPoints() {
        Track track = TestDataUtil.createTrack(new Track.Id(0));
        Track.Id trackId = contentProviderUtils.insertTrack(track);

        TestDataUtil.insertTrackWithLocations(contentProviderUtils, track,
                TestDataUtil.createTrackPoints(10));

        // Verify trackpoints exist
        int countBefore;
        ContentResolver cr = context.getContentResolver();
        try (Cursor cursor = cr.query(TrackPointsColumns.CONTENT_URI_BY_ID,
                new String[]{TrackPointsColumns._ID},
                DbUtils.eqClause(TrackPointsColumns.TRACKID),
                DbUtils.idArgs(trackId.id()), null)) {
            countBefore = cursor != null ? cursor.getCount() : 0;
        }
        assertTrue(countBefore > 0);

        // Delete track
        contentProviderUtils.deleteTrack(context, trackId);

        // Verify trackpoints were cascade-deleted
        int countAfter;
        try (Cursor cursor = cr.query(TrackPointsColumns.CONTENT_URI_BY_ID,
                new String[]{TrackPointsColumns._ID},
                DbUtils.eqClause(TrackPointsColumns.TRACKID),
                DbUtils.idArgs(trackId.id()), null)) {
            countAfter = cursor != null ? cursor.getCount() : 0;
        }
        assertEquals(0, countAfter);
    }

    /**
     * Tests that deleting a track cascades to its markers.
     */
    @Test
    public void testDelete_cascadeMarkers() {
        Track track = TestDataUtil.createTrack(new Track.Id(0));
        Track.Id trackId = contentProviderUtils.insertTrack(track);

        // Insert a marker for this track
        Track trackWithMarkers = TestDataUtil.createTrack(trackId);
        TestDataUtil.createTestingTrack(contentProviderUtils, trackWithMarkers);

        // Verify markers exist
        int countBefore;
        ContentResolver cr = context.getContentResolver();
        try (Cursor cursor = cr.query(MarkerColumns.CONTENT_URI,
                new String[]{MarkerColumns._ID},
                DbUtils.eqClause(MarkerColumns.TRACKID),
                DbUtils.idArgs(trackId.id()), null)) {
            countBefore = cursor != null ? cursor.getCount() : 0;
        }
        assertTrue(countBefore > 0);

        // Delete track
        contentProviderUtils.deleteTrack(context, trackId);

        // Verify markers were cascade-deleted
        int countAfter;
        try (Cursor cursor = cr.query(MarkerColumns.CONTENT_URI,
                new String[]{MarkerColumns._ID},
                DbUtils.eqClause(MarkerColumns.TRACKID),
                DbUtils.idArgs(trackId.id()), null)) {
            countAfter = cursor != null ? cursor.getCount() : 0;
        }
        assertEquals(0, countAfter);
    }

    /**
     * Tests update through the BY_ID URI.
     */
    @Test
    public void testUpdate_byId() {
        Track track = TestDataUtil.createTrack(new Track.Id(0));
        Track.Id trackId = contentProviderUtils.insertTrack(track);

        track.setId(trackId);
        track.setName("Updated Name");
        contentProviderUtils.updateTrack(track);

        Track loaded = contentProviderUtils.getTrack(trackId);
        assertNotNull(loaded);
        assertEquals("Updated Name", loaded.getName());
    }
}