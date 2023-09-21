package com.opencanarias.taple.android.db.sqlite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.opencanarias.taple.ffi.DatabaseManagerInterface
import com.opencanarias.taple.ffi.DbCollectionInterface

class SqLiteManager(private val context: Context, db_name: String) : DatabaseManagerInterface,
    SQLiteOpenHelper(context, db_name, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        //context.deleteDatabase(DATABASE_NAME);
        // below is a sqlite query, where column names
        // along with their data types is given
        val query = ("CREATE TABLE " + TABLE_NAME + " (" +
                ID_COL + " TEXT PRIMARY KEY," +
                VALUE_COL + " MEDIUMBLOB)")
        // we are calling sqlite
        // method for executing our query
        db.execSQL(query)
        val query2 = ("CREATE INDEX SQL_TAPLE ON " + TABLE_NAME +  " (" + ID_COL + ")");
        db.execSQL(query2)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
        onCreate(db)
    }

    override fun createCollection(identifier: String): DbCollectionInterface {
        val db = this.writableDatabase
        return SqLiteDatabaseCollection(db)
    }

    companion object {
        // here we have defined variables for our database
        // below is variable for database name

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

/*
onUpgrade() is called (you do not call it yourself) when version
of your DB changed which means underlying table structure changed etc.

In general it means that OS is telling you "hey, you asked for database
structure version 10 but I found we got something older here, so this is
you chance to fix that before you start using database (and potentially
crash due to structure mismatch)".

In that method you should do all that is necessary to, well.. upgrade
structure of your old database to structure current version requirements
like adding/droping columns, converting row contents or even dropping old
db completely and create it from scratch - for Android it does not matter
what you do here - it's just a sort of emergency callback for your code to
do the necessary job (if any). You need to be aware that users may not
update frequently so you have to always handle upgrade from version X to Y
knowing that X may not be equal to i.e. (Y-1).
 */


