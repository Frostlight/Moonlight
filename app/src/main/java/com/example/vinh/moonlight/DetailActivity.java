package com.example.vinh.moonlight;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;

import com.example.vinh.moonlight.data.WeatherContract;
import com.example.vinh.moonlight.data.WeatherContract.WeatherEntry;


public class DetailActivity extends ActionBarActivity  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);

        //Share action button
        // Get the menu item.
        MenuItem menuItem = menu.findItem(R.id.share);

        //Set the provider for sharing.
        ShareActionProvider mShareActionProvider = new ShareActionProvider(this);
        MenuItemCompat.setActionProvider(menuItem, mShareActionProvider);
        //Log.v(App.getTag(), "Share action provider: " + mShareActionProvider.toString());

        //Since we already have the data to share, just set the ShareActionProvider to the weather text
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, this.getIntent().getStringExtra(Intent.EXTRA_TEXT) + " #MoonlightApp");
        mShareActionProvider.setShareIntent(shareIntent);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //noinspection SimplifiableIfStatement

        switch (item.getItemId()) {
            case R.id.action_settings:
                //Explicit intent to start settings
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                return true;
            case R.id.share:
                //share button already handled in onCreateOptionsMenu()
                //do nothing
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A fragment containing a detail view.
     */
    public static class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
        public static int DETAIL_LOADER = 0;
        private static final String[] FORECAST_COLUMNS = {
                // In this case the id needs to be fully qualified with a table name, since
                // the content provider joins the location & weather tables in the background
                // (both have an _id column)
                // On the one hand, that's annoying.  On the other, you can search the weather table
                // using the location set by the user, which is only in the Location table.
                // So the convenience is worth it.
                WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
                WeatherContract.WeatherEntry.COLUMN_DATE,
                WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
                //WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
                //WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                //WeatherContract.LocationEntry.COLUMN_COORD_LAT,
                //WeatherContract.LocationEntry.COLUMN_COORD_LONG
        };

        // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
        // must change.
        static final int COL_WEATHER_ID = 0;
        static final int COL_WEATHER_DATE = 1;
        static final int COL_WEATHER_DESC = 2;
        static final int COL_WEATHER_MAX_TEMP = 3;
        static final int COL_WEATHER_MIN_TEMP = 4;

        public String forecastStr;

        public DetailFragment() {
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
            Intent intent = getActivity().getIntent();
            if (intent != null)
            {
                forecastStr = intent.getDataString();
                ((TextView) rootView.findViewById(R.id.detail_text)).setText(forecastStr);
            }
            return rootView;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Uri weatherUri = Uri.parse(forecastStr);
            //Log.v(App.getTag(), "weatherUri: " + weatherUri.toString());

            //sort order: ascending by date
            String sortOrder = WeatherEntry.COLUMN_DATE + " ASC";


//        //Log.v(App.getTag(), "onCreateLoader()");
//        String locationSetting = Utility.getPreferredLocation(getActivity());
//        //sort order: ascending by date
//        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
//        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
//                locationSetting, System.currentTimeMillis());
//
        return new CursorLoader(getActivity(),
                weatherUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            //Regenerate the detail output by using data from the cursor
            //Display it on the textview
            if (data.moveToFirst()) {
                String date = Utility.formatDate(data.getLong(COL_WEATHER_DATE));
                //Log.v(App.getTag(), "Date: " + date);

                String desc = data.getString(COL_WEATHER_DESC);
                //Log.v(App.getTag(), "Desc: " + desc);

                Boolean isMetric = Utility.isMetric(getActivity());
                String max_temp = Utility.formatTemperature(
                        data.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
                String min_temp = Utility.formatTemperature(
                        data.getDouble(COL_WEATHER_MIN_TEMP), isMetric);

                String display_string = date + " - " + desc + " - " + max_temp + "/" + min_temp;
                //Log.v(App.getTag(), "Display string: " + display_string);

                TextView detailView = (TextView) getView().findViewById(R.id.detail_text);
                detailView.setText(display_string);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    }





}
