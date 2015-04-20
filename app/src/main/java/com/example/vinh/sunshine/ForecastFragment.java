package com.example.vinh.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
* Created by Vincent on 4/13/2015.
*/
public class ForecastFragment extends Fragment {

    static ArrayAdapter<String> adapter;

    public ForecastFragment() {
    }

    //update weather based on location in preferences
    private void updateWeatherTask(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String location = prefs.getString(getString(R.string.pref_location_key),
                        getString(R.string.pref_location_default));
                //Log.v(App.getTag(), "Location: "+location);
                new FetchWeatherTask().execute(location);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //allows fragment to handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeatherTask();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        //inflate the menu option
        inflater.inflate(R.menu.forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_refresh:
                updateWeatherTask();
            return true;
            //case R.id.action_settings:
            //    return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        adapter = new ArrayAdapter<>(getActivity(),
                R.layout.list_item_forecast, R.id.list_item_forecast_textview, new ArrayList<String>());

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);

        try {
            listView.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                //show a toast with the weather information
                @Override
                public void onItemClick(AdapterView<?> AdapterView, View view, int i, long l)
                {
                    //Legacy toast, launches detail activity instead
                    /*CharSequence text = AdapterView.getItemAtPosition(i).toString();
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(view.getContext(), text, duration);
                    toast.show();*/

                    //Explicit intent to start DetailActivity with the weather information
                    Intent details = new Intent(view.getContext(), DetailActivity.class); //DetailActivity.class);
                    //details.setData(Uri.parse(AdapterView.getItemAtPosition(i).toString()));
                    details.putExtra(Intent.EXTRA_TEXT, AdapterView.getItemAtPosition(i).toString());
                    startActivity(details);
                }
            }
        );
        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]>
    {
        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);

                //Get unit preference from settings
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String units = prefs.getString(getString(R.string.pref_unit_key),
                        getString(R.string.pref_unit_default));


                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                //A few logging statements to verify celsius/fahrenheit functioning properly
                //Log.v(App.getTag(), "Units: "+units);
                //Log.v(App.getTag(), getResources().getStringArray(R.array.unit_values)[0]); //celsius
                //Log.v(App.getTag(), getResources().getStringArray(R.array.unit_values)[1]); //fahrenheit

                //if units settings is fahrenheit, need to convert high and low accordingly
                if (units.compareTo(getResources().getStringArray(R.array.unit_values)[1]) == 0) {
                    high = 9*high/5 + 32;
                    low = 9*low/5 + 32;
                }

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            //Log forecast entries (formatted)
            /*for (String s : resultStrs) {
                Log.v(App.getTag(), "Forecast entry: " + s);
            }*/
            return resultStrs;

        }


        //clear and repopulate adapter with new weather information
        @Override
        protected void onPostExecute(String[] strings) {
            if (strings != null)
            {
                adapter.clear();
                for(String day : strings)
                {
                    adapter.add(day);
                }
            }
        }

        @Override
        protected String[] doInBackground(String... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            //query values (changeable)
            String format = "json";
            String units = "metric";
            int numDays = 7;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7");

                //the base url and query names
                final String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String MODE_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String NUM_DAYS_PARAM = "cnt";

                //build the URL using uri builder
                Uri built_uri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(MODE_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(NUM_DAYS_PARAM, Integer.toString(numDays))
                        .build();
                URL url = new URL(built_uri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();

                //Log JSON received from openweathermap server
                //Log.v(App.getTag(), "forecastJsonStr: " + forecastJsonStr);
            } catch (IOException e) {
                //error
                Log.e("PlaceholderFragment", Log.getStackTraceString(new Exception()));

                //Log.d("myapp", Log.getStackTraceString(new Exception()));
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        //Error closing stream
                        Log.e("PlaceholderFragment", Log.getStackTraceString(new Exception()));
                    }
                }
            }


            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(App.getTag(), e.getMessage(), e);
                e.printStackTrace();
            }



            //will only happen if there was an error getting weather data from JSON
            return null;
        }
    }
}
