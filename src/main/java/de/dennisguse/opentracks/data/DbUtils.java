package de.dennisguse.opentracks.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Shared database operation helpers.
 * Centralizes transaction boilerplate, SQL clause construction, and cursor iteration patterns.
 */
class DbUtils {

    private DbUtils() {}

    /**
     * Runs an action inside a database transaction.
     * Replaces the repeated beginTransaction / setTransactionSuccessful / endTransaction pattern.
     */
    static void runInTransaction(SQLiteDatabase db, Runnable action) {
        db.beginTransaction();
        try {
            action.run();
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Runs an action inside a database transaction and returns a result.
     */
    static <T> T runInTransaction(SQLiteDatabase db, Supplier<T> action) {
        db.beginTransaction();
        try {
            T result = action.get();
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Builds a SQL IN clause like {@code column IN (?,?,?)}.
     *
     * @param column the column name
     * @param count  the number of placeholders
     */
    static String buildInClause(String column, int count) {
        return column + " IN (" + String.join(",", Collections.nCopies(count, "?")) + ")";
    }

    /**
     * Builds a where clause for BY_ID updates: {@code idColumn=id [AND (extraWhere)]}.
     *
     * @param idColumn   the ID column name
     * @param id         the row ID
     * @param extraWhere optional additional where clause (may be null or empty)
     */
    static String buildWhereById(String idColumn, long id, String extraWhere) {
        String whereClause = idColumn + "=" + id;
        if (extraWhere != null && !extraWhere.isEmpty()) {
            whereClause += " AND (" + extraWhere + ")";
        }
        return whereClause;
    }

    /**
     * Converts long IDs to a String array suitable for selection arguments.
     * Replaces the repeated {@code Long.toString(id.id())} pattern.
     */
    static String[] idArgs(long... ids) {
        String[] args = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            args[i] = Long.toString(ids[i]);
        }
        return args;
    }

    /**
     * Builds an equality clause: {@code column=?}.
     */
    static String eqClause(String column) {
        return column + "=?";
    }

    /**
     * Iterates a cursor and collects rows into a list using the provided mapper.
     * The cursor is closed automatically when done.
     *
     * @param cursor the cursor to iterate (will be closed)
     * @param mapper function to convert a cursor row to an element
     */
    static <T> List<T> cursorToList(Cursor cursor, Function<Cursor, T> mapper) {
        ArrayList<T> list = new ArrayList<>();
        try (cursor) {
            if (cursor != null && cursor.moveToFirst()) {
                list.ensureCapacity(cursor.getCount());
                do {
                    list.add(mapper.apply(cursor));
                } while (cursor.moveToNext());
            }
        }
        return list;
    }
}
