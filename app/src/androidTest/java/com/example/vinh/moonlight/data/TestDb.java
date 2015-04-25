/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.vinh.moonlight.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import java.util.HashSet;

public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    // Since we want each test to start with a clean slate
    void deleteTheDatabase() {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }

    /*
        This function gets called before each test is executed to delete the database.  This makes
        sure that we always have a clean test.
     */
    public void setUp() {
        deleteTheDatabase();
    }

    /*
        Students: Uncomment this test once you've written the code to create the Location
        table.  Note that you will have to have chosen the same column names that I did in
        my solution for this test to compile, so if you haven't yet done that, this is
        a good time to change your column names to match mine.

        Note that this only tests that the Location table has the correct columns, since we
        give you the code for the weather table.  This test does not look at the
     */
    public void testCreateDb() throws Throwable {
        // build a HashSet of all of the table names we wish to look for
        // Note that there will be another table in the DB that stores the
        // Android metadata (db version information)
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(WeatherContract.LocationEntry.TABLE_NAME);
        tableNameHashSet.add(WeatherContract.WeatherEntry.TABLE_NAME);

        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while( c.moveToNext() );

        // if this fails, it means that your database doesn't contain both the location entry
        // and weather entry tables
        assertTrue("Error: Your database was created without both the location entry and weather entry tables",
                tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + WeatherContract.LocationEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> locationColumnHashSet = new HashSet<String>();
        locationColumnHashSet.add(WeatherContract.LocationEntry._ID);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_CITY_NAME);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LAT);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LONG);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            locationColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required location entry columns",
                locationColumnHashSet.isEmpty());
        db.close();
    }

    /*
        Insert and query the location database.
    */
    public void testLocationTable() {
        //Get reference to writable database
        WeatherDbHelper dbHelper = new WeatherDbHelper(this.getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        //insert location into location db
        insertLocation(db,  TestUtilities.createNorthPoleLocationValues());
        db.close();
    }

    /*
        Insert and query the database (both location and weather)
     */
    public void testWeatherTable() {
        // First insert the location, and then use the locationRowId to insert
        // the weather. Make sure to cover as many failure cases as you can.
        WeatherDbHelper dbHelper = new WeatherDbHelper(this.getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        //insert location into location db
        long locationRowId = insertLocation(db, TestUtilities.createNorthPoleLocationValues());

        // Create ContentValues of what we want to insert
        // (you can use the createWeatherValues TestUtilities function if you wish)
        ContentValues weatherValues = TestUtilities.createWeatherValues(locationRowId);

        // Insert ContentValues into database and get a row ID back
        long weatherRowId = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, weatherValues);

        // Verify insertion was successful
        assertTrue("Error: Insertion was unsuccessful", weatherRowId != -1);

        // Query the database and receive a Cursor back
        //Table: ID | Location Settings | City Name | Longitude | Latitude
        Cursor c = db.rawQuery("SELECT * FROM " + WeatherContract.WeatherEntry.TABLE_NAME, null);

        // Move the cursor to a valid database row, error if unable to
        assertTrue("Error: This means that we were unable to query the database for table information",
                c.moveToFirst());

        // Validate data in resulting Cursor with the original ContentValues
        //get index of location
        long location_index = c.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_LOC_KEY);

        //The first entry should be the one we added to the database
        //Log.v(App.getTag(), "Location: " + c.getString(location_index));
        assertTrue("Error: First entry was not equal to entry just inserted",
                location_index == locationRowId);
        //Log.v(App.getTag(), "locationRowId: " + locationRowId + " location_index: " + location_index);

        //c.moveToNext should fail since there is only one entry
        assertTrue("Error: More than one entry in table", !c.moveToNext());

        // Finally, close the cursor and database
        c.close();
        db.close();
    }


    /*
        Helper method for testWeatherTable and testLocationTable
        Returns the location key so testWeatherTable can use it
     */
    public long insertLocation(SQLiteDatabase db, ContentValues testValues) {
        String testLocationSetting = testValues.getAsString
                (WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING);
        //Log.v(App.getTag(), "testLocationSetting: " + testLocationSetting);

        // Insert ContentValues into database and get a row ID back
        long locationRowId = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, testValues);

        // Verify insertion was successful
        assertTrue("Error: Insertion was unsuccessful", locationRowId != -1);

        // Query the database and receive a Cursor back
        //Table: ID | Location Settings | City Name | Longitude | Latitude
        Cursor c = db.rawQuery("SELECT * FROM " + WeatherContract.LocationEntry.TABLE_NAME, null);

        // Move the cursor to a valid database row, error if unable to
        assertTrue("Error: This means that we were unable to query the database for table information",
                c.moveToFirst());

        // Validate data in resulting Cursor with the original ContentValues
        //Table: [0]ID | [1]Location Settings | [2]City Name | [3]Longitude | [4]Latitude
        //Log.v(App.getTag(), "ToString: " + c.getColumnName(0) + c.getColumnName(1) + c.getColumnName(2));

        //get index of location
        int location_index = c.getColumnIndex(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING);

        //The first entry should be the one we added to the database
        //Log.v(App.getTag(), "Location: " + c.getString(location_index));
        assertTrue("Error: First entry was not equal to entry just inserted",
                c.getString(location_index).equals(testLocationSetting));

        //c.moveToNext should fail since there is only one entry
        assertTrue("Error: More than one entry in table", !c.moveToNext());

        // Finally, close the cursor and database
        c.close();
        return locationRowId;
    }
}
