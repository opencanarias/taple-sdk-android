package com.opencanarias.taple.android.db.sqlite

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.opencanarias.taple.ffi.DbException
import com.opencanarias.taple.ffi.DbCollectionInterface
import com.opencanarias.taple.ffi.DbCollectionIteratorInterface


internal class SqLiteDatabaseCollection(db: SQLiteDatabase): DbCollectionInterface {

    var sqlite_db: SQLiteDatabase = db

    override fun get(key: String): List<UByte>? {
        val sql_query = "SELECT " +  VALUE_COL +
                        " FROM " + TABLE_NAME +
                        " WHERE TRIM("+ ID_COL + ") = '" + key.trim() + "'"
        val c: Cursor =
            sqlite_db.rawQuery(sql_query, null)
        if (c.count == 0) {
            return null
        }
        c.moveToFirst()
        val tmp = c.getColumnIndex(VALUE_COL);
        if (tmp >= 0) {
            return c.getBlob(tmp)
                .toUByteArray().toList()
        } else {
            throw DbException.KeyElementsException("Invalid Table");
        }
    }

    override fun put(key: String, value: List<UByte>) {
        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(ID_COL, key)
        values.put(VALUE_COL, value.toUByteArray().toByteArray())
        // Insert the new row, returning the primary key value of the new row
        sqlite_db.replace(TABLE_NAME, null, values)
    }

    override fun del(key: String) {
        val WHERE = ID_COL + " = " + "?"
        sqlite_db.delete(TABLE_NAME, WHERE, arrayOf(key))
    }

    override fun iter(reverse: Boolean, prefix: String): DbCollectionIteratorInterface {
        val sql_query = "SELECT " + ID_COL + ", " + VALUE_COL +
                " FROM " + TABLE_NAME +
                " WHERE TRIM(" + ID_COL + ") LIKE '" + prefix.trim() + "%'" +
                " ORDER BY " + ID_COL
        val result = sqlite_db.rawQuery(sql_query, null);
        return if (reverse) {
            SqliteRevIterator(result)
        } else {
            SqliteIterator(result)
        }
    }

    companion object {
        // here we have defined variables for our database
        // below is variable for database name
        private val DATABASE_NAME = "TAPLE_DB"

        // below is the variable for database version
        private val DATABASE_VERSION = 1

        // below is the variable for table name
        val TABLE_NAME = "TAPLE_TABLE"

        // below is the variable for id column
        val ID_COL = "id"

        // below is the variable for name column
        val VALUE_COL = "value"
    }
}
