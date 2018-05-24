package app.com.weatherapplication.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class CustomSharedPreference {

    private SharedPreferences sharedPref;

    private Gson gson;

    public CustomSharedPreference(Context context) {
        sharedPref = context.getSharedPreferences(Helper.PREFS_TAG, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void setDataSourceIfPresent(boolean isData){
        sharedPref.edit().putBoolean(Helper.IS_DATA_PRESENT, isData).apply();
    }

    public boolean getDataSourceIfPresent(){
        return sharedPref.getBoolean(Helper.IS_DATA_PRESENT, false);
    }

    public void setLocationInPreference(String cityName){
        sharedPref.edit().putString(Helper.LOCATION_PREFS, cityName).apply();
    }

    public String getLocationInPreference(){
        return sharedPref.getString(Helper.LOCATION_PREFS, "");
    }

    public void setUnitInPreference(String unit){
        sharedPref.edit().putString(Helper.UNIT_PREFS, unit).apply();
    }

    public String getUnitInPreference(){
        return sharedPref.getString(Helper.UNIT_PREFS, "");
    }
}
