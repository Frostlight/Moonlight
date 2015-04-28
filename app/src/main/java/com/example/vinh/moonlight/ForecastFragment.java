package com.example.vinh.moonlight;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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

        //Call FetchWeatherTask to update weather
        new FetchWeatherTask(getActivity(), adapter).execute(location);
        super.onStart();
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rootView;
    }
}
