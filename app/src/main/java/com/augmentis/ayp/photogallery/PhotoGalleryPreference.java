package com.augmentis.ayp.photogallery;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Noppharat on 8/19/2016.
 */
public class PhotoGalleryPreference {
    private static final String TAG = "PhotoGalleryPreference";
    protected static final String PREF_SEARCH_KEY = "PhotoGalleryPreference";
    protected static final String PREF_LAST_ID = "PREF_LAST_ID";

    public static SharedPreferences mySharedPref(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String getStoredSearchKey(Context context){
//        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return mySharedPref(context).getString(PREF_SEARCH_KEY, null);
    }

    public static void setStoredSearchKey(Context context, String key){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit()
                .putString(PREF_SEARCH_KEY, key)
                .apply();
    }

    public static String getStoredLastId(Context context){
//        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return mySharedPref(context).getString(PREF_LAST_ID, null);
    }

    public static void setStoredLastId(Context context, String lastId){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit()
                .putString(PREF_LAST_ID, lastId)
                .apply();
    }
}
