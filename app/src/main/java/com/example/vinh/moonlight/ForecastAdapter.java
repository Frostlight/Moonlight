package com.example.vinh.moonlight;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import com.example.vinh.moonlight.data.WeatherContract;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {
    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;
    private static final int VIEW_TYPE_COUNT = 2;
    private boolean mUseTodayLayout;

    /**
     * Pipeline the boolean from MainActivity to determine whether or not
     * to use today's layout (true if phone, false if tablet)
     * @param useTodayLayout Whether or not today's layout is used
     */
    public void setUseTodayLayout(boolean useTodayLayout)
    {
        mUseTodayLayout = useTodayLayout;
    }

    // Disable today's layout if the device is a tablet
    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Choose the layout type
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;
        if (viewType == VIEW_TYPE_TODAY)
            layoutId = R.layout.list_item_today;
        else if (viewType == VIEW_TYPE_FUTURE_DAY)
            layoutId = R.layout.list_item_forecast;
        View view =  LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        boolean isMetric = Utility.isMetric(mContext);
        String highLowStr = Utility.formatTemperature(mContext, high, isMetric) + "/" + Utility.formatTemperature(mContext, low, isMetric);
        return highLowStr;
    }

    /**
     * Fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Make a ViewHolder for the current view
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Get Weather condition ID from cursor, set weather icon using it
        int weatherID = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int viewType = getItemViewType(cursor.getPosition());
        switch(viewType)
        {
            // Use different assets for different viewtypes
            case VIEW_TYPE_FUTURE_DAY:
                viewHolder.iconView.setImageResource(Utility.getIconResourceForWeatherCondition(weatherID));
                break;
            case VIEW_TYPE_TODAY:
                viewHolder.iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherID));
        }

        // Get date from cursor
        long dateInMillis = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        viewHolder.dateView.setText(Utility.getFriendlyDayString(context, dateInMillis));

        // Get forecast from cursor
        String description = cursor.getString(ForecastFragment.COL_WEATHER_DESC);
        viewHolder.descriptionView.setText(description);

        // Read preference for metric/imperial
        boolean isMetric = Utility.isMetric(context);

        // Get high from cursor
        double high = cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        viewHolder.highTempView.setText(Utility.formatTemperature(context, high, isMetric));

        // Get low from cursor
        double low = cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        viewHolder.lowTempView.setText(Utility.formatTemperature(context, low, isMetric));
    }


    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;


        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);

        }
    }
}