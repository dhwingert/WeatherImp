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
package com.example.android.sunshine.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.example.android.sunshine.data.SunshinePreferences;
import com.example.android.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility functions to handle AccuWeather.com JSON data.
 */
public final class AccuWeatherJsonUtils {

    private static final String TAG = AccuWeatherJsonUtils.class.getSimpleName();

    // ACCUWEATHER LOCATION API RETURN VALUE JSON KEYS
    private static final String AW_LOCATION_KEY = "Key";
    private static final String AW_LOCATION_KEY_TYPE = "Type";
    private static final String AW_ENGLISH_NAME = "EnglishName";
    private static final String AW_DATASETS = "DataSets";

    // ACCUWEATHER FORECAST API RETURN VALUE JSON KEYS
    private static final String AW_HEADLINE = "Headline";
    private static final String AW_DATE_EFFECTIVE = "EffectiveDate";
    private static final String AW_DATE_END = "EndDate";
    private static final String AW_SEVERITY = "Severity";
    private static final String AW_TEXT = "Text";
    private static final String AW_CATEGORY = "Category";
    private static final String AW_FORECASTS_DAILY = "DailyForecasts";
    private static final String AW_DATE = "Date";
    private static final String AW_TEMPERATURE = "Temperature";
    private static final String AW_MINIMUM = "Minimum";
    private static final String AW_MAXIMUM = "Maximum";
    private static final String AW_VALUE = "Value";
    private static final String AW_UNIT = "Unit";
    private static final String AW_UNIT_TYPE = "UnitType";
    private static final String AW_ENGLISH_DESC = "English";
    private static final String AW_LOCALIZED_DESC = "Localized";
    private static final String AW_DAYTIME = "Day";
    private static final String AW_NIGHTTIME = "Night";
    private static final String AW_ICON_NUM = "Icon";
    private static final String AW_ICON_PHRASE = "IconPhrase";

    private static final String AW_WIND_FORECAST = "Wind";
    private static final String AW_WIND_SPEED = "Speed";
    private static final String AW_WIND_DIRECTION = "Direction";
    private static final String AW_WIND_DIR_DEGREES = "Degrees";

    private static final String AW_PRECIP_PROB = "PrecipitationProbability";
    private static final String AW_PRECIP_HOURS = "HoursOfPrecipitation";

    /* Location information */
    private static final String OWM_CITY = "city";
    private static final String OWM_COORD = "coord";

    /* Location coordinate */
    private static final String OWM_LATITUDE = "lat";
    private static final String OWM_LONGITUDE = "lon";

    /* Weather information. Each day's forecast info is an element of the "list" array */
    private static final String OWM_LIST = "list";

    private static final String OWM_PRESSURE = "pressure";
    private static final String OWM_HUMIDITY = "humidity";
    private static final String OWM_WINDSPEED = "speed";
    private static final String OWM_WIND_DIRECTION = "deg";

    /* All temperatures are children of the "temp" object */
    private static final String OWM_TEMPERATURE = "temp";

    /* Max temperature for the day */
    private static final String OWM_MAX = "max";
    private static final String OWM_MIN = "min";

    private static final String OWM_WEATHER = "weather";
    private static final String OWM_WEATHER_ID = "id";

    private static final String OWM_MESSAGE_CODE = "cod";

    // This method parses JSON from a web response and returns the location info that will be used
    // to query for the weather at that location.
    public static String getLocationFromJson(Context context, String locationJsonStr) throws JSONException {

        // Default to null, location not found
        String locationKey = null;

        // If the web service returned location data...
        if (locationJsonStr != null && locationJsonStr.length() > 0) {
            Log.v(TAG, "AccuWeather locationJson: " + locationJsonStr);

            // AccuWeather returns location as a JSON array.
            // Need to wrap it in JSON with a root key so we can parse it
            String jsonWrapper = "{\"root\":" + locationJsonStr + "}";
            Log.v(TAG, "AccuWeather jsonWrapper: " + jsonWrapper);

            // Now get the array of locations from the root node in the JSON
            JSONObject wrapperJson = new JSONObject(jsonWrapper);
            JSONArray jsonRootArray = wrapperJson.getJSONArray("root");

            if (jsonRootArray.length() > 0) {
                JSONObject locationJson = jsonRootArray.getJSONObject(0);

                locationKey = locationJson.getString(AW_LOCATION_KEY);
                String locationKeyType = locationJson.getString(AW_LOCATION_KEY_TYPE);
                String englishName = locationJson.getString(AW_ENGLISH_NAME);
                String datasets = locationJson.getString(AW_DATASETS);

                Log.v(TAG, "AccuWeather locationKey: " + locationKey);
                Log.v(TAG, "AccuWeather locationKeyType: " + locationKeyType);
                Log.v(TAG, "AccuWeather englishName: " + englishName);
                Log.v(TAG, "AccuWeather datasets: " + datasets);
            }
        }


//        /* Is there an error? */
//        if (locationJson.has(OWM_MESSAGE_CODE)) {
//            int errorCode = locationJson.getInt(OWM_MESSAGE_CODE);
//
//            switch (errorCode) {
//                case HttpURLConnection.HTTP_OK:
//                    break;
//                case HttpURLConnection.HTTP_NOT_FOUND:
//                    /* Location invalid */
//                    return null;
//                default:
//                    /* Server probably down */
//                    return null;
//            }
//        }


        return locationKey;
    }

    /**
     * This method parses JSON from a web response and returns an array of Strings
     * describing the weather over various days from the forecast.
     * <p/>
     * Later on, we'll be parsing the JSON into structured data within the
     * getFullWeatherDataFromJson function, leveraging the data we have stored in the JSON. For
     * now, we just convert the JSON into human-readable strings.
     *
     * @param forecastJsonStr JSON response from server
     *
     * @return Array of Strings describing weather data
     *
     * @throws JSONException If JSON data cannot be properly parsed
     */
    public static ContentValues[] getWeatherContentValuesFromJson(Context context, String forecastJsonStr)
            throws JSONException {

        ContentValues[] weatherContentValues = null;

        // If the web service returned location data...
        if (forecastJsonStr != null && forecastJsonStr.length() > 0) {
            Log.v(TAG, "AccuWeather forecastJson: " + forecastJsonStr);

            JSONObject forecastJson = new JSONObject(forecastJsonStr);

//            /* Is there an error? */
//            if (forecastJson.has(OWM_MESSAGE_CODE)) {
//                int errorCode = forecastJson.getInt(OWM_MESSAGE_CODE);
//
//                switch (errorCode) {
//                    case HttpURLConnection.HTTP_OK:
//                        break;
//                    case HttpURLConnection.HTTP_NOT_FOUND:
//                        /* Location invalid */
//                        return null;
//                    default:
//                        /* Server probably down */
//                        return null;
//                }
//            }

            JSONArray jsonWeatherArray = forecastJson.getJSONArray(AW_FORECASTS_DAILY);

//            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
//
//            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
//            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
//            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);
//
//            SunshinePreferences.setLocationDetails(context, cityLatitude, cityLongitude);

            weatherContentValues = new ContentValues[jsonWeatherArray.length()];

            /*
             * OWM returns daily forecasts based upon the local time of the city that is being asked
             * for, which means that we need to know the GMT offset to translate this data properly.
             * Since this data is also sent in-order and the first day is always the current day, we're
             * going to take advantage of that to get a nice normalized UTC date for all of our weather.
             */
            //        long now = System.currentTimeMillis();
            //        long normalizedUtcStartDay = SunshineDateUtils.normalizeDate(now);

            long normalizedUtcStartDay = SunshineDateUtils.getNormalizedUtcDateForToday();

            for (int i = 0; i < jsonWeatherArray.length(); i++) {

                long dateTimeMillis;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;
                String winDirDesc;

                double high;
                double low;

                int weatherId;
                String weatherDesc;

                double precipProb;
                double precipHours;

                /* Get the JSON object representing the 24 hour forecast */
                JSONObject forecast24hr = jsonWeatherArray.getJSONObject(i);

                // Get the day and night forecasts from the 24 hour forecast
                JSONObject forecastDaytime = forecast24hr.getJSONObject(AW_DAYTIME);
                JSONObject forecastNighttime = forecast24hr.getJSONObject(AW_NIGHTTIME);

                /*
                 * We ignore all the datetime values embedded in the JSON and assume that
                 * the values are returned in-order by day (which is not guaranteed to be correct).
                 */
                dateTimeMillis = normalizedUtcStartDay + SunshineDateUtils.DAY_IN_MILLIS * i;

                //DHW - Fake data until I figure out how to get it from AccuWeather's JSON
//                pressure = 0; // forecast24hr.getDouble(OWM_PRESSURE);
//                humidity = 0; // forecast24hr.getInt(OWM_HUMIDITY);

                /*
                 * Description is in a child array called "weather", which is 1 element long.
                 * That element also contains a weather code.
                 */
//                JSONObject weatherObject =
//                        forecast24hr.getJSONArray(OWM_WEATHER).getJSONObject(0);
//
//                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Overall type of weather for the day/night.
                weatherId = forecastDaytime.getInt(AW_ICON_NUM);
                weatherDesc = forecastDaytime.getString(AW_ICON_PHRASE);

                // Wind
                JSONObject windForecast = forecastDaytime.getJSONObject(AW_WIND_FORECAST);
                JSONObject windSpeedObj = windForecast.getJSONObject(AW_WIND_SPEED);
                windSpeed = windSpeedObj.getDouble(AW_VALUE);
                JSONObject windDirObj = windForecast.getJSONObject(AW_WIND_DIRECTION);
                windDirection = windDirObj.getDouble(AW_WIND_DIR_DEGREES);
                winDirDesc = windDirObj.getString(AW_LOCALIZED_DESC);

                // High and Low temperatures for the entire 24 hour day are in "Temperature"
                JSONObject temperature24Hrs = forecast24hr.getJSONObject(AW_TEMPERATURE);
                JSONObject temperature24HrsMin = temperature24Hrs.getJSONObject(AW_MINIMUM);
                JSONObject temperature24HrsMax = temperature24Hrs.getJSONObject(AW_MAXIMUM);
                high = temperature24HrsMax.getDouble(AW_VALUE);
                low = temperature24HrsMin.getDouble(AW_VALUE);

                // Overall chance of precipitation
                precipProb = forecastDaytime.getDouble(AW_PRECIP_PROB);
                precipHours = forecastDaytime.getDouble(AW_PRECIP_HOURS);

                ContentValues weatherValues = new ContentValues();
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTimeMillis);
//                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
//                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES_DESC, winDirDesc);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_DESC, weatherDesc);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRECIP_PROB, precipProb);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRECIP_HOURS, precipHours);

                weatherContentValues[i] = weatherValues;
            }
        }

        return weatherContentValues;
    }
}