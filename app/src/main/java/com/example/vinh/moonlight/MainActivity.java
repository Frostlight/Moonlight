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

public class MainActivity extends ActionBarActivity {
    public String mLocation;
    public boolean mTwoPane;
    private final String DETAILFRAGMENT_TAG = "DFTAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (findViewById(R.id.weather_detail_container) != null)
        {
            Log.v(App.getTag(), "TWO PANE MODE, onCreate");
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
        else
            mTwoPane = false;
        mLocation = Utility.getPreferredLocation(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //If current location is not the same as in settings, update to new location
        String pref_location = Utility.getPreferredLocation(getApplicationContext());
        if (!pref_location.equals(mLocation))
        {
            ForecastFragment ff = (ForecastFragment)getSupportFragmentManager().
                    findFragmentById(R.id.fragment_forecast);

            //updates weather and restarts the loader
            ff.onLocationChanged();
            mLocation = pref_location;
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
