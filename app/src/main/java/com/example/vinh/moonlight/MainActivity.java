package com.example.vinh.moonlight;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback {
    public String mLocation;
    public boolean mTwoPane;
    private final String DETAILFRAGMENT_TAG = "DFTAG";

    @Override
    public void onItemSelected(Uri contentUri) {
        if (mTwoPane) {
            //Log.v(App.getTag(), "TwoPane onItemSelected");
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI, contentUri);
            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            //Log.v(App.getTag(), "OnePane onItemSelected");
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(contentUri);
            startActivity(intent);
       }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (findViewById(R.id.weather_detail_container) != null)
        {
            //Log.v(App.getTag(), "TWO PANE MODE, onCreate");
            //Detail container view is only present in large screen layouts
            //(tablet). if this is true, activity should be in two-pane mode
            mTwoPane = true;

            //initialize detail fragment with transaction
            if (savedInstanceState == null)
            {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, new DetailFragment())
                        .commit();
            }
        }
        else {
            mTwoPane = false;
            getSupportActionBar().setElevation(0f);
        }
        mLocation = Utility.getPreferredLocation(this);

        //pipeline to ForecastAdapter whether or not to use today's view
        //depending on two pane or one pane (tablet or phone)
        ForecastFragment forecastFragment = ((ForecastFragment) getSupportFragmentManager()
            .findFragmentById(R.id.fragment_forecast));
        forecastFragment.setUseTodayLayout(!mTwoPane);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String location = Utility.getPreferredLocation( this );
        // update the location in our second pane using the fragment manager
        if (location != null && !location.equals(mLocation)) {
            ForecastFragment ff = (ForecastFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
            if ( null != ff ) {
                ff.onLocationChanged();
            }
            DetailFragment df = (DetailFragment)getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if ( null != df ) {
                df.onLocationChanged(location);
            }
            mLocation = location;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //Source: https://developer.android.com/guide/components/intents-common.html#Maps
    //Implicit intent to open maps with a specified geolocation
    public void showMap(Uri geoLocation) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            //Explicit intent to start settings
            case R.id.action_settings:
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                return true;
            //Implicit intent to open maps with preferred location (from preferences)
            case R.id.view_map:
                //the base url and query names
                //format is geo:0,0?q=(preferred location)
                final String QUERY_PARAM = "q";

                //get preferred location from preferences
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                String location = prefs.getString(getString(R.string.pref_location_key),
                        getString(R.string.pref_location_default));
                //Log.v(App.getTag(), "Location: " + location);

                //build the uri and launch intent
                Uri built_uri = Uri.parse("geo:"+Uri.encode("0,0")+"?").buildUpon()
                        .appendQueryParameter(QUERY_PARAM, location)
                        .build();
                //Log.v(App.getTag(), "Built uri: " + built_uri.toString());
                showMap(built_uri);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
