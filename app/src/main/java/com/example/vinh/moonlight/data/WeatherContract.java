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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;

/**
 * Defines table and column names for the weather database.
 */
public class WeatherContract {
    // Use the Content Authority as the name of the package of the App
    public static final String CONTENT_AUTHORITY = "com.example.vinh.moonlight";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // E.g. content://com.example.vinh.moonlight/weather/ for weather data
    public static final String PATH_WEATHER = "weather";
    public static final String PATH_LOCATION = "location";

    /**
     * Normalize dates for the database to the start of the the Julian day at UTC.
     * @param startDate Regular date
     * @return Normalized Julian day at UTC
     */
    public static long normalizeDate(long startDate) {
        Time time = new Time();
        time.set(startDate);
        int julianDay = Time.getJulianDay(startDate, time.gmtoff);
        return time.setJulianDay(julianDay);
    }

    /**
     * Inner class that defines the table contents of the location table
     * Table: ID | Location Settings | City Name | Longitude | Latitude
     */
    public static final class LocationEntry implements BaseColumns {
        /** ----------------  Uri definitions and functions ---------------- */
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LOCATION;

        /**
         * Builds a Uri for the LocationEntry table with a specified ID
         * @param id ID of Uri to build
         * @return Resultant Uri
         */
        public static Uri buildLocationUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        /** ------------------------  Table Columns ------------------------ */
        public static final String TABLE_NAME = "location";

        // Location setting; can be a ZIP code, postal code, address, city, etc.
        public static final String COLUMN_LOCATION_SETTING = "location_setting";

        // The city of the location
        public static final String COLUMN_CITY_NAME = "city_name";

        // Longitude coordinates of the location
        public static final String COLUMN_COORD_LONG = "longitude";

        // Latitude coordinates of the location
        public static final String COLUMN_COORD_LAT = "latitude";
    }

    /**
     * Inner class that defines the table contents of the weather table
     * Table: ID | Location Settings | City Name | Longitude | Latitude
     */
    public static final class WeatherEntry implements BaseColumns {
        /** ----------------  Uri definitions and functions ---------------- */
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_WEATHER).build();
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_WEATHER;

        /**
         * Builds a Uri for the WeatherEntry table with a specified ID
         * @param id ID of Uri to build
         * @return Resultant Uri
         */
        public static Uri buildWeatherUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        /**
         * Builds a Uri for the WeatherEntry table with a specified location setting
         * E.g. locationSetting = "Toronto" builds CONTENT_URI/Toronto
         * @param locationSetting Location for Uri to build
         * @return Resultant Uri
         */
        public static Uri buildWeatherLocation(String locationSetting) {
            return CONTENT_URI.buildUpon().appendPath(locationSetting).build();
        }

        /**
         * Builds a Uri for the WeatherEntry table with a specified location setting AND start date
         * Uri returns weather for a certain number of upcoming days (up to ten)
         * @param locationSetting Location for Uri to build
         * @param startDate Starting date to get weather from
         * @return Resultant Uri
         */
        public static Uri buildWeatherLocationWithStartDate(
                String locationSetting, long startDate) {
            long normalizedDate = normalizeDate(startDate);
            return CONTENT_URI.buildUpon().appendPath(locationSetting)
                    .appendQueryParameter(COLUMN_DATE, Long.toString(normalizedDate)).build();
        }

        /**
         * Builds a Uri for the WeatherEntry table with a specified location setting AND exact date
         * Uri returns weather for a single date
         * @param locationSetting Location for Uri to build
         * @param date Date to get weather from
         * @return Resultant Uri
         */
        public static Uri buildWeatherLocationWithDate(String locationSetting, long date) {
            return CONTENT_URI.buildUpon().appendPath(locationSetting)
                    .appendPath(Long.toString(normalizeDate(date))).build();
        }

        /**
         * Gets the starting date from a WeatherEntry Uri
         * Related to function buildWeatherLocationWithStartDate()
         * @param uri input Uri
         * @return Starting date in long form
         */
        public static long getStartDateFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if (null != dateString && dateString.length() > 0)
                return Long.parseLong(dateString);
            else
                return 0;
        }

        /**
         * Gets the exact date from a WeatherEntry Uri
         * Related to function buildWeatherLocationWithDate()
         * @param uri input Uri
         * @return Exact date in long form
         */
        public static long getDateFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }

        /**
         * Gets the location setting from a WeatherEntry Uri
         * @param uri input Uri
         * @return Location setting in String form
         */
        public static String getLocationSettingFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /** ------------------------  Table Columns ------------------------ */
        public static final String TABLE_NAME = "weather";

        // Column with the foreign key into the location table.
        public static final String COLUMN_LOC_KEY = "location_id";

        // Date, stored as long in milliseconds since the epoch
        public static final String COLUMN_DATE = "date";

        // Weather id as returned by API, to identify the icon to be used
        public static final String COLUMN_WEATHER_ID = "weather_id";

        // Short description and long description of the weather, as provided by API.
        // e.g "clear" vs "sky is clear".
        public static final String COLUMN_SHORT_DESC = "short_desc";

        // Min and max temperatures for the day (stored as floats)
        public static final String COLUMN_MIN_TEMP = "min";
        public static final String COLUMN_MAX_TEMP = "max";

        // Humidity is stored as a float representing percentage
        public static final String COLUMN_HUMIDITY = "humidity";

        // Humidity is stored as a float representing percentage
        public static final String COLUMN_PRESSURE = "pressure";

        // Windspeed is stored as a float representing windspeed  mph
        public static final String COLUMN_WIND_SPEED = "wind";

        // Degrees are meteorological degrees (e.g, 0 is north, 180 is south).  Stored as floats.
        public static final String COLUMN_DEGREES = "degrees";
    }
}
