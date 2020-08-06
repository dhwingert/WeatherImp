/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.example.android.sunshine.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.databinding.tool.util.StringUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.example.android.sunshine.data.SunshinePreferences;
import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.AccuWeatherJsonUtils;
import com.example.android.sunshine.utilities.NetworkUtils;
import com.example.android.sunshine.utilities.NotificationUtils;
import com.example.android.sunshine.utilities.OpenWeatherJsonUtils;

import java.net.URL;

public class SunshineSyncTask {

    private static final String TAG = SunshineSyncTask.class.getSimpleName();

    /**
     * Performs the network request for updated weather, parses the JSON from that request, and
     * inserts the new weather information into our ContentProvider. Will notify the user that new
     * weather has been loaded if the user hasn't been notified of the weather within the last day
     * AND they haven't disabled notifications in the preferences screen.
     *
     * @param context Used to access utility methods and the ContentResolver
     */
    synchronized public static void syncWeather(Context context) {

        try {
            /*
             * The getUrl method will return the URL that we need to get the forecast JSON for the
             * weather. It will decide whether to create a URL based off of the latitude and
             * longitude or off of a simple location as a String.
             */
//            URL weatherRequestUrl = NetworkUtils.getUrl(context);
//
//            /* Use the URL to retrieve the JSON */
//            /* Parse the JSON into a list of weather values */
//            String jsonWeatherResponse = NetworkUtils.getResponseFromHttpUrl(weatherRequestUrl);
//            Log.v(TAG, "OpenWeather Weather Response: " + jsonWeatherResponse);
//            ContentValues[] weatherValues = OpenWeatherJsonUtils.getWeatherContentValuesFromJson(context, jsonWeatherResponse);

            ContentValues[] weatherValues = null;

            // ACCUWEATHER.COM
            // First need to get the location info
            //      Build the URL to get the location info
            //      Query the URL for the location info
            /* Use the URL to retrieve the JSON */
            /* Use the URL to retrieve the JSON */
            URL locationUrl = NetworkUtils.getAccuWeatherLocationUrl(context);
            Log.v(TAG, "AccuWeather Location URL: " + locationUrl);
            String jsonLocationResponse = NetworkUtils.getResponseFromHttpUrl(locationUrl);
            Log.v(TAG, "AccuWeather Location Response: " + jsonLocationResponse);

            if (jsonLocationResponse != null && jsonLocationResponse.length() != 0) {
                String locationKey = AccuWeatherJsonUtils.getLocationFromJson(context, jsonLocationResponse);
                if (locationKey != null && locationKey.length() > 0) {
                    URL forecastUrl = NetworkUtils.getAccuWeatherForecastUrl(context, locationKey);
                    Log.v(TAG, "AccuWeather Forecast URL: " + forecastUrl);
                    String jsonForecastResponse = NetworkUtils.getResponseFromHttpUrl(forecastUrl);
                    Log.v(TAG, "AccuWeather Forecast Response: " + jsonForecastResponse);

                    // Replace fake weather demo data with actual weather data from AccuWeather
                    weatherValues = AccuWeatherJsonUtils.getWeatherContentValuesFromJson(context, jsonForecastResponse);
                }
            }

            /*
             * In cases where our JSON contained an error code, getWeatherContentValuesFromJson
             * would have returned null. We need to check for those cases here to prevent any
             * NullPointerExceptions being thrown. We also have no reason to insert fresh data if
             * there isn't any to insert.
             */
            if (weatherValues != null && weatherValues.length != 0) {
                /* Get a handle on the ContentResolver to delete and insert data */
                ContentResolver sunshineContentResolver = context.getContentResolver();

                /* Delete old weather data because we don't need to keep multiple days' data */
                sunshineContentResolver.delete(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        null,
                        null);

                /* Insert our new weather data into Sunshine's ContentProvider */
                sunshineContentResolver.bulkInsert(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        weatherValues);

                /*
                 * Finally, after we insert data into the ContentProvider, determine whether or not
                 * we should notify the user that the weather has been refreshed.
                 */
                boolean notificationsEnabled = SunshinePreferences.areNotificationsEnabled(context);

                /*
                 * If the last notification was shown was more than 1 day ago, we want to send
                 * another notification to the user that the weather has been updated. Remember,
                 * it's important that you shouldn't spam your users with notifications.
                 */
                long timeSinceLastNotification = SunshinePreferences
                        .getEllapsedTimeSinceLastNotification(context);

                boolean oneDayPassedSinceLastNotification = false;

                if (timeSinceLastNotification >= DateUtils.DAY_IN_MILLIS) {
                    oneDayPassedSinceLastNotification = true;
                }

                /*
                 * We only want to show the notification if the user wants them shown and we
                 * haven't shown a notification in the past day.
                 */
                if (notificationsEnabled && oneDayPassedSinceLastNotification) {
                    NotificationUtils.notifyUserOfNewWeather(context);
                }

            /* If the code reaches this point, we have successfully performed our sync */

            }

        } catch (Exception e) {
            /* Server probably invalid */
            e.printStackTrace();
        }
    }
}