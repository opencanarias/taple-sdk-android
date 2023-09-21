package com.opencanarias.taple.android.db.sqlite

import android.database.Cursor
import com.opencanarias.taple.ffi.DbCollectionIteratorInterface
import com.opencanarias.taple.ffi.Tuple

class SqliteIterator: DbCollectionIteratorInterface {

    var cursor: Cursor;
    var n_elements: Int;
    var count: Int;

    constructor(cursor: Cursor) {
        this.n_elements = cursor.count
        cursor.moveToFirst()
        this.cursor = cursor
        this.count = 0
    }

    override fun next(): Tuple? {
        if (count >= n_elements) {
            return null;
        } else {
            this.count++
            val key = this.cursor.getString(0)
            val value = this.cursor.getBlob(1).toUByteArray().toList()
            this.cursor.moveToNext()
            return Tuple(key, value)
        }
    }

}

class SqliteRevIterator: DbCollectionIteratorInterface {

    var cursor: Cursor;
    var n_elements: Int;
    var count: Int;

    constructor(cursor: Cursor) {
        this.n_elements = cursor.count
        cursor.moveToLast()
        this.cursor = cursor
        this.count = 0
    }

    override fun next(): Tuple? {
        if (count >= n_elements) {
            return null;
        } else {
            this.count++
            val key = this.cursor.getString(0)
            val value = this.cursor.getBlob(1).toUByteArray().toList()
            this.cursor.moveToPrevious()
            return Tuple(key, value)
        }
    }

}
