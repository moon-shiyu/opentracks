package de.dennisguse.opentracks.data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Pure JVM tests for {@link DbUtils} methods that don't depend on Android APIs.
 */
public class DbUtilsTest {

    @Test
    public void testBuildInClause_single() {
        assertEquals("_id IN (?)", DbUtils.buildInClause("_id", 1));
    }

    @Test
    public void testBuildInClause_multiple() {
        assertEquals("_id IN (?,?,?)", DbUtils.buildInClause("_id", 3));
    }

    @Test
    public void testBuildWhereById_noExtraWhere() {
        assertEquals("_id=42", DbUtils.buildWhereById("_id", 42, null));
    }

    @Test
    public void testBuildWhereById_emptyExtraWhere() {
        assertEquals("_id=42", DbUtils.buildWhereById("_id", 42, ""));
    }

    @Test
    public void testBuildWhereById_withExtraWhere() {
        assertEquals("_id=42 AND (name=?)", DbUtils.buildWhereById("_id", 42, "name=?"));
    }

    @Test
    public void testIdArgs_single() {
        assertArrayEquals(new String[]{"42"}, DbUtils.idArgs(42));
    }

    @Test
    public void testIdArgs_multiple() {
        assertArrayEquals(new String[]{"1", "2", "3"}, DbUtils.idArgs(1, 2, 3));
    }

    @Test
    public void testIdArgs_empty() {
        assertArrayEquals(new String[]{}, DbUtils.idArgs());
    }

    @Test
    public void testEqClause() {
        assertEquals("trackid=?", DbUtils.eqClause("trackid"));
    }
}
