package com.deffe.max.chatgoo.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.deffe.max.chatgoo.R;

public class PermissionUtil
{
    private Context context;
    private SharedPreferences sharedPreferences;

    public static final String PERMISSION_ACCESS_NETWORK_STATE = "ACCESS_NETWORK_STATE";
    public static final String PERMISSION_INTERNET = "INTERNET";
    public static final String PERMISSION_RECORD_AUDIO = "RECORD_AUDIO";
    public static final String PERMISSION_CHANGE_NETWORK_STATE = "CHANGE_NETWORK_STATE";
    public static final String PERMISSION_CHANGE_WIFI_STATE = "CHANGE_WIFI_STATE";
    public static final String PERMISSION_ACCESS_WIFI_STATE = "ACCESS_WIFI_STATE";
    public static final String PERMISSION_BLUETOOTH = "BLUETOOTH";
    public static final String PERMISSION_BLUETOOTH_ADMIN = "BLUETOOTH_ADMIN";
    public static final String PERMISSION_ACCESS_FINE_LOCATION = "ACCESS_FINE_LOCATION";
    public static final String PERMISSION_READ_CONTACTS = "READ_CONTACTS";
    public static final String PERMISSION_WRITE_CONTACTS = "WRITE_CONTACTS";
    public static final String PERMISSION_CALL_PHONE = "CALL_PHONE";
    public static final String PERMISSION_READ_EXTERNAL_STORAGE = "READ_EXTERNAL_STORAGE";
    public static final String PERMISSION_WRITE_EXTERNAL_STORAGE = "WRITE_EXTERNAL_STORAGE";
    public static final String PERMISSION_CAMERA = "CAMERA";
    public static final String PERMISSION_SET_ALARM = "SET_ALARM";

    public static final int READ_ACCESS_NETWORK_STATE = 1;
    public static final int READ_INTERNET = 2;
    public static final int READ_RECORD_AUDIO = 3;
    public static final int READ_CHANGE_NETWORK_STATE = 4;
    public static final int READ_CHANGE_WIFI_STATE = 5;
    public static final int READ_ACCESS_WIFI_STATE = 6;
    public static final int READ_BLUETOOTH = 7;
    public static final int READ_BLUETOOTH_ADMIN = 8;
    public static final int READ_ACCESS_FINE_LOCATION = 9;
    public static final int READ_READ_CONTACTS = 10;
    public static final int READ_CALL_PHONE = 11;
    public static final int READ_READ_EXTERNAL_STORAGE = 12;
    public static final int READ_WRITE_EXTERNAL_STORAGE = 13;
    public static final int READ_CAMERA = 14;
    public static final int READ_SET_ALARM = 15;
    public static final int READ_WRITE_CONTACTS = 31;

    public static final int REQUEST_ACCESS_NETWORK_STATE = 16;
    public static final int REQUEST_INTERNET = 17;
    public static final int REQUEST_RECORD_AUDIO = 18;
    public static final int REQUEST_CHANGE_NETWORK_STATE = 19;
    public static final int REQUEST_CHANGE_WIFI_STATE = 20;
    public static final int REQUEST_ACCESS_WIFI_STATE = 21;
    public static final int REQUEST_BLUETOOTH = 22;
    public static final int REQUEST_BLUETOOTH_ADMIN = 23;
    public static final int REQUEST_ACCESS_FINE_LOCATION = 24;
    public static final int REQUEST_READ_CONTACTS = 25;
    public static final int REQUEST_CALL_PHONE = 26;
    public static final int REQUEST_READ_EXTERNAL_STORAGE = 27;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE = 28;
    public static final int REQUEST_CAMERA = 29;
    public static final int REQUEST_SET_ALARM = 30;
    public static final int REQUEST_WRITE_CONTACTS = 32;

    public PermissionUtil(Context context)
    {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.permission_preference),Context.MODE_PRIVATE);
    }

    public void updatePermissionPreference(String permission)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        switch (permission)
        {
            case PERMISSION_ACCESS_NETWORK_STATE:
                editor.putBoolean(context.getString(R.string.permission_ACCESS_NETWORK_STATE),true);
                editor.apply();
                break;
            case PERMISSION_INTERNET:
                editor.putBoolean(context.getString(R.string.permission_INTERNET),true);
                editor.apply();
                break;
            case PERMISSION_RECORD_AUDIO:
                editor.putBoolean(context.getString(R.string.permission_RECORD_AUDIO),true);
                editor.apply();
                break;
            case PERMISSION_CHANGE_NETWORK_STATE:
                editor.putBoolean(context.getString(R.string.permission_CHANGE_NETWORK_STATE),true);
                editor.apply();
                break;
            case PERMISSION_CHANGE_WIFI_STATE:
                editor.putBoolean(context.getString(R.string.permission_CHANGE_WIFI_STATE),true);
                editor.apply();
                break;
            case PERMISSION_ACCESS_WIFI_STATE:
                editor.putBoolean(context.getString(R.string.permission_ACCESS_WIFI_STATE),true);
                editor.apply();
                break;
            case PERMISSION_BLUETOOTH:
                editor.putBoolean(context.getString(R.string.permission_BLUETOOTH),true);
                editor.apply();
                break;
            case PERMISSION_BLUETOOTH_ADMIN:
                editor.putBoolean(context.getString(R.string.permission_BLUETOOTH_ADMIN),true);
                editor.apply();
                break;
            case PERMISSION_ACCESS_FINE_LOCATION:
                editor.putBoolean(context.getString(R.string.permission_ACCESS_FINE_LOCATION),true);
                editor.apply();
                break;
            case PERMISSION_READ_CONTACTS:
                editor.putBoolean(context.getString(R.string.permission_READ_CONTACTS),true);
                editor.apply();
                break;
            case PERMISSION_CALL_PHONE:
                editor.putBoolean(context.getString(R.string.permission_CALL_PHONE),true);
                editor.apply();
                break;
            case PERMISSION_READ_EXTERNAL_STORAGE:
                editor.putBoolean(context.getString(R.string.permission_READ_EXTERNAL_STORAGE),true);
                editor.apply();
                break;
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                editor.putBoolean(context.getString(R.string.permission_WRITE_EXTERNAL_STORAGE),true);
                editor.apply();
                break;
            case PERMISSION_CAMERA:
                editor.putBoolean(context.getString(R.string.permission_CAMERA),true);
                editor.apply();
                break;
            case PERMISSION_SET_ALARM:
                editor.putBoolean(context.getString(R.string.permission_SET_ALARM),true);
                editor.apply();
                break;
            case PERMISSION_WRITE_CONTACTS:
                editor.putBoolean(context.getString(R.string.permission_WRITE_CONTACTS),true);
                editor.apply();
                break;
        }
    }

    public boolean checkPermissionPreference(String permission)
    {
        boolean isShown = false;

        switch (permission)
        {
            case PERMISSION_ACCESS_NETWORK_STATE:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_ACCESS_NETWORK_STATE),false);
                break;
            case PERMISSION_INTERNET:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_INTERNET),false);
                break;
            case PERMISSION_RECORD_AUDIO:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_RECORD_AUDIO),false);
                break;
            case PERMISSION_CHANGE_NETWORK_STATE:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_CHANGE_NETWORK_STATE),false);
                break;
            case PERMISSION_CHANGE_WIFI_STATE:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_CHANGE_WIFI_STATE),false);
                break;
            case PERMISSION_ACCESS_WIFI_STATE:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_ACCESS_WIFI_STATE),false);
                break;
            case PERMISSION_BLUETOOTH:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_BLUETOOTH),false);
                break;
            case PERMISSION_BLUETOOTH_ADMIN:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_BLUETOOTH_ADMIN),false);
                break;
            case PERMISSION_ACCESS_FINE_LOCATION:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_ACCESS_FINE_LOCATION),false);
                break;
            case PERMISSION_READ_CONTACTS:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_READ_CONTACTS),false);
                break;
            case PERMISSION_CALL_PHONE:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_CALL_PHONE),false);
                break;
            case PERMISSION_READ_EXTERNAL_STORAGE:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_READ_EXTERNAL_STORAGE),false);
                break;
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_WRITE_EXTERNAL_STORAGE),false);
                break;
            case PERMISSION_CAMERA:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_CAMERA),false);
                break;
            case PERMISSION_SET_ALARM:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_SET_ALARM),false);
                break;
            case PERMISSION_WRITE_CONTACTS:
                isShown = sharedPreferences.getBoolean(context.getString(R.string.permission_WRITE_CONTACTS),false);
                break;
        }
        return !isShown;
    }
}