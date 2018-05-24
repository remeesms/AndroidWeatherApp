package app.com.weatherapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.github.pavlospt.CircleView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.com.weatherapplication.controllers.RecyclerViewAdapter;
import app.com.weatherapplication.helpers.CustomSharedPreference;
import app.com.weatherapplication.helpers.Helper;
import app.com.weatherapplication.model.FiveWeathers;
import app.com.weatherapplication.model.Forecast;
import app.com.weatherapplication.model.LocationMapObject;
import app.com.weatherapplication.model.WeatherObject;

public class MainActivity extends AppCompatActivity implements android.location.LocationListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView recyclerView;

    private RecyclerViewAdapter recyclerViewAdapter;

    private TextView cityCountry;

    private TextView currentDate;

    private CircleView circleTitle;

    private TextView windResult;

    private TextView humidityResult;

    private LocationManager locationManager;

    private Location location;

    private final int REQUEST_LOCATION = 200;

    private CustomSharedPreference sharedPreference;

    private String apiUrl;

    private LocationMapObject locationMapObject;

    private ImageButton share_data,change_unit;

    private ProgressDialog pDialog;

    private String shareBodyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.hide();
        }
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Please wait...");

        sharedPreference = new CustomSharedPreference(MainActivity.this);

        cityCountry = (TextView)findViewById(R.id.city_country);
        currentDate = (TextView)findViewById(R.id.current_date);
        circleTitle = (CircleView)findViewById(R.id.weather_result);
        windResult = (TextView)findViewById(R.id.wind_result);
        humidityResult = (TextView)findViewById(R.id.humidity_result);
        share_data = (ImageButton) findViewById(R.id.share_data);
        change_unit = (ImageButton) findViewById(R.id.change_unit);

        share_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");

                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Today's Weather");
                intent.putExtra(android.content.Intent.EXTRA_TEXT, shareBodyText);
                startActivity(Intent.createChooser(intent, "Share Weather"));
            }
        });

        change_unit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String weatherTemp;
                Long tempVal;
                if(sharedPreference.getUnitInPreference().equals("") || sharedPreference.getUnitInPreference().equals("f") ){
                    tempVal = Math.round(Math.floor(Double.parseDouble(locationMapObject.getMain().getTemp())));
                    weatherTemp = String.valueOf(tempVal) + "°c";
                    circleTitle.setTitleText(Html.fromHtml(weatherTemp).toString());
                    sharedPreference.setUnitInPreference("c");
                }else{
                    tempVal = Math.round(Math.floor(Double.parseDouble(locationMapObject.getMain().getTemp())));
                    Long tempInF = Math.round(Math.floor(1.8*tempVal +32));
                    weatherTemp = String.valueOf(tempInF) + "°f";
                    circleTitle.setTitleText(Html.fromHtml(weatherTemp).toString());
                    sharedPreference.setUnitInPreference("f");
                }


            }
        });

        locationManager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            // make API call with longitude and latitude
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 2, this);
            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if(location == null){
                    showGPSDisabledAlertToUser();
                }else{
                    apiUrl = "http://api.openweathermap.org/data/2.5/weather?lat="+location.getLatitude()+"&lon="+location.getLongitude()+"&APPID="+ Helper.API_KEY+"&units=metric";
                    makeJsonObject(apiUrl);
                }

            }

        }



        GridLayoutManager gridLayoutManager = new GridLayoutManager(MainActivity.this, 4);

        recyclerView = (RecyclerView)findViewById(R.id.weather_daily_list);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setHasFixedSize(true);

    }

    private void makeJsonObject(final String apiUrl){
        showpDialog();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, apiUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Response " + response);
                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();
                locationMapObject = gson.fromJson(response, LocationMapObject.class);
                if (null == locationMapObject) {
                    Toast.makeText(getApplicationContext(), "Nothing was returned", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Response Good", Toast.LENGTH_LONG).show();

                    String city = locationMapObject.getName() + ", " + locationMapObject.getSys().getCountry();
                    String todayDate = getTodayDateInStringFormat();
                    String weatherTemp;
                    Long tempVal;
                    if(sharedPreference.getUnitInPreference().equals("") || sharedPreference.getUnitInPreference().equals("c") ){
                        tempVal = Math.round(Math.floor(Double.parseDouble(locationMapObject.getMain().getTemp())));
                        weatherTemp = String.valueOf(tempVal) + "°c";
                        circleTitle.setTitleText(Html.fromHtml(weatherTemp).toString());
                    }else{
                        tempVal = Math.round(Math.floor(Double.parseDouble(locationMapObject.getMain().getTemp())));
                        Long tempInF = Math.round(Math.floor(1.8*tempVal +32));
                        weatherTemp = String.valueOf(tempInF) + "°f";
                        circleTitle.setTitleText(Html.fromHtml(weatherTemp).toString());
                    }
                    String weatherDescription = Helper.capitalizeFirstLetter(locationMapObject.getWeather().get(0).getDescription());
                    String windSpeed = locationMapObject.getWind().getSpeed();
                    String humidityValue = locationMapObject.getMain().getHumudity();

                    // populate View data
                    cityCountry.setText(Html.fromHtml(city));
                    currentDate.setText(Html.fromHtml(todayDate));
                    circleTitle.setSubtitleText(Html.fromHtml(weatherDescription).toString());
                    windResult.setText(Html.fromHtml(windSpeed) + " km/h");
                    humidityResult.setText(Html.fromHtml(humidityValue) + " %");

                    shareBodyText = "City: " + Html.fromHtml(city) +"\n" + "Date: " + Html.fromHtml(todayDate) +"\n"+"Temp: " + Html.fromHtml(weatherTemp).toString()+","+ Html.fromHtml(weatherDescription).toString()+"\n"+"Wind: " + Html.fromHtml(windSpeed) + " km/h" +"\n" + "Humidity: "+Html.fromHtml(humidityValue) + " %";

                    threeDaysApiJsonObjectCall(locationMapObject.getName());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error " + error.getMessage());
            }
        });
        WeatherApplication.getInstance().getRequestQueue().add(stringRequest);
    }

    private void threeDaysApiJsonObjectCall(String city){
        String apiUrl = "http://api.openweathermap.org/data/2.5/forecast?q="+city+ "&APPID="+Helper.API_KEY+"&units=metric";
        Log.d(TAG, "apiUrl" + apiUrl);

        final List<WeatherObject> daysOfTheWeek = new ArrayList<WeatherObject>();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, apiUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Response 5 days" + response);
                hidepDialog();
                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();
                Forecast forecast = gson.fromJson(response, Forecast.class);
                if (null == forecast) {
                    Toast.makeText(getApplicationContext(), "Nothing was returned", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Response Good", Toast.LENGTH_LONG).show();

                    int[] everyday = new int[]{0,0,0,0,0,0,0};

                    List<FiveWeathers> weatherInfo = forecast.getList();
                    if(null != weatherInfo){
                        for(int i = 0; i < weatherInfo.size(); i++){
                            String time = weatherInfo.get(i).getDt_txt();
                            String shortDay = convertTimeToDay(time);
                            String temp = weatherInfo.get(i).getMain().getTemp();
                            String tempMin = weatherInfo.get(i).getMain().getTemp_min();

                            if(convertTimeToDay(time).equals("Mon") && everyday[0] < 1){
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[0] = 1;
                            }
                            if(convertTimeToDay(time).equals("Tue") && everyday[1] < 1){
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[1] = 1;
                            }
                            if(convertTimeToDay(time).equals("Wed") && everyday[2] < 1){
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[2] = 1;
                            }
                            if(convertTimeToDay(time).equals("Thu") && everyday[3] < 1){
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[3] = 1;
                            }
                            if(convertTimeToDay(time).equals("Fri") && everyday[4] < 1){
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[4] = 1;
                            }
                            if(convertTimeToDay(time).equals("Sat") && everyday[5] < 1){
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[5] = 1;
                            }
                            if(convertTimeToDay(time).equals("Sun") && everyday[6] < 1){
                                daysOfTheWeek.add(new WeatherObject(shortDay, R.drawable.small_weather_icon, temp, tempMin));
                                everyday[6] = 1;
                            }
                            recyclerViewAdapter = new RecyclerViewAdapter(MainActivity.this, daysOfTheWeek);
                            recyclerView.setAdapter(recyclerViewAdapter);
                        }
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error " + error.getMessage());
            }
        });
        WeatherApplication.getInstance().getRequestQueue().add(stringRequest);

    }


    private String getTodayDateInStringFormat(){
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("E, d MMMM", Locale.getDefault());
        return df.format(c.getTime());
    }

    private String convertTimeToDay(String time){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:SSSS", Locale.getDefault());
        String days = "";
        try {
            Date date = format.parse(time);
            System.out.println("Our time " + date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            days = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault());
            System.out.println("Our time " + days);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return days;
    }

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Goto Settings Page To Enable GPS", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(callGPSSettingIntent);
                    }
                });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {
        showGPSDisabledAlertToUser();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    private void showpDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hidepDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }
}
