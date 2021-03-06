package com.example.vinh.moonlight;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.vinh.moonlight.data.WeatherContract;

import org.w3c.dom.Text;

/**
 * A fragment containing a detail view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    public static int DETAIL_LOADER = 0;
    static final String DETAIL_URI = "URI";
    public Uri mUri;

    // Member variables: the ShareActionProvider and references to all the fragment's views
    private ShareActionProvider mShareActionProvider;
    private ImageView mIconView;
    private TextView mDayNameView;
    private TextView mDateView;
    private TextView mDescriptionView;
    private TextView mHighTempView;
    private TextView mLowTempView;
    private TextView mHumidityView;
    private TextView mWindView;
    private TextView mPressureView;
    private String mDisplayString;

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
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_HUMIDITY = 5;
    static final int COL_WINDSPEED = 6;
    static final int COL_DEGREES = 7;
    static final int COL_PRESSURE = 8;
    static final int COL_WEATHER_CONDITION_ID = 9;

    public DetailFragment() {
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_detailfragment, menu);

        // Get the action provider for the share menu button, so we can set the share intent with it
        // Two cases: 1) loader initialises first 2) onCreateOptionsMenu initialises first
        //   1) if the loader initialises first, we just set the share intent here, since
        //      we already have the string to share from the loader
        //   2) if onCreateOptionsMenu initialises first (this), we'll set mShareActionProvider now
        //      and let the cursor loader set the share intent later
        MenuItem menuItem = menu.findItem(R.id.share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        if (mDisplayString != null) {
            // This means the loader finished initializing first
            // We just set the share intent here in that case
            Intent shareIntent = createIntent(mDisplayString);
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Get Uri from callback
        Bundle arguments = getArguments();
        if (arguments != null)
            mUri = arguments.getParcelable(DETAIL_URI);

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        //Set up references to each view as member variables so we can change them later
        mIconView = (ImageView) rootView.findViewById(R.id.list_item_icon);
        mDayNameView = (TextView) rootView.findViewById(R.id.list_item_dayname_textview);
        mDateView = (TextView) rootView.findViewById(R.id.list_item_date_textview);
        mDescriptionView = (TextView) rootView.findViewById(R.id.list_item_forecast_textview);
        mHighTempView = (TextView) rootView.findViewById(R.id.list_item_high_textview);
        mLowTempView = (TextView) rootView.findViewById(R.id.list_item_low_textview);
        mHumidityView = (TextView) rootView.findViewById(R.id.list_item_humidity_textview);
        mWindView = (TextView) rootView.findViewById(R.id.list_item_wind_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.list_item_pressure_textview);
        return rootView;
    }

    /**
     * Prepare the Uri, since the location has changed
     * @param newLocation The new location that the user changed it to
     */
    void onLocationChanged( String newLocation ) {
        // Prepare the uri, since the location has changed
        Uri uri = mUri;
        if (uri != null) {
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    /**
     * Creates a new share intent with the display string sent as text
     * @param display_string The display string sent as text in the intent
     * @return The intent that was created
     */
    private Intent createIntent(String display_string)
    {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, display_string + " #MoonlightApp");
        return shareIntent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mUri != null)
            return new CursorLoader(getActivity(),
                    mUri,
                    FORECAST_COLUMNS,
                    null,
                    null,
                    null);
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Regenerate the detail output by using data from the cursor
        // Display it on the TextView
        if (data.moveToFirst()) {
            // Set day name textview
            long date = data.getLong(COL_WEATHER_DATE);
            // TextView dayNameView = (TextView) getView().findViewById(R.id.list_item_dayname_textview);
            mDayNameView.setText(Utility.getDayName(getActivity(), date));

            // Set date textview
            mDateView.setText(Utility.getFormattedMonthDay(getActivity(), date));

            // Set high and low temperature textviews
            Boolean isMetric = Utility.isMetric(getActivity());
            String max_temp = Utility.formatTemperature(
                    getActivity(), data.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
            String min_temp = Utility.formatTemperature(
                    getActivity(), data.getDouble(COL_WEATHER_MIN_TEMP), isMetric);
            mHighTempView.setText(max_temp);
            mLowTempView.setText(min_temp);

            // Get Weather condition ID from cursor, set weather image using it
            int weatherID = data.getInt(COL_WEATHER_CONDITION_ID);
            mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherID));

            // Set weather description
            String description = data.getString(COL_WEATHER_DESC);
            mDescriptionView.setText(description);

            // Set humidity textview
            double humidity = data.getDouble(COL_HUMIDITY);
            // Humidity is invisible if zero (that means data wasn't available)
            if (humidity == 0)
                mHumidityView.setVisibility(View.GONE);
            else
                mHumidityView.setText(Utility.formatHumidity(getActivity(), humidity));

            // Set wind speed and direction textview (windspeed is in mph)
            double windSpeed = data.getDouble(COL_WINDSPEED);
            double windDegrees = data.getDouble(COL_DEGREES);
            mWindView.setText(Utility.formatWind(getActivity(), windSpeed, windDegrees));

            // Set pressure textview
            mPressureView.setText(Utility.formatPressure(getActivity(), data.getDouble(COL_PRESSURE)));

            // Set the text used by the ShareActionProvider
            String displayString = Utility.formatDate(date) + " - " + description + " - " +
                    max_temp + "/" + min_temp;

            if (mShareActionProvider != null) {
                // This means onCreateOptionsMenu already executed, so set the share intent here
                Intent shareIntent = createIntent(displayString);
                mShareActionProvider.setShareIntent(shareIntent);
            } else {
                // Otherwise, initialise a member variable mDisplayString so onCreateOptionsMenu
                // can initialise the shareIntent instead
                mDisplayString = displayString;
            }
        }
    }

    // Empty because no data is held that needs to be cleaned up
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}


}
