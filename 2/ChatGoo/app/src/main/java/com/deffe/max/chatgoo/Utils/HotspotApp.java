package com.deffe.max.chatgoo.Utils;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;

public class HotspotApp
{
    public static boolean isApOn(Context context)
    {
        WifiManager wifimanager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try
        {
            Method method = null;

            if (wifimanager != null)
            {
                method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            }

            if (method != null)
            {
                method.setAccessible(true);
                return (Boolean) method.invoke(wifimanager);
            }
        }
        catch (Throwable ignored) {}
        return false;
    }

    public static boolean configApState(Context context,String status)
    {
        WifiManager wifimanager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        try
        {
            if(isApOn(context))
            {
                if (wifimanager != null)
                {
                    if (status.equals("enable"))
                    {
                        wifimanager.setWifiEnabled(true);
                    }
                    else
                    {
                        wifimanager.setWifiEnabled(false);
                    }
                }
            }
            Method method = null;

            if (wifimanager != null)
            {
                method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            }

            if (method != null)
            {
                method.invoke(wifimanager, null, !isApOn(context));
            }
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
