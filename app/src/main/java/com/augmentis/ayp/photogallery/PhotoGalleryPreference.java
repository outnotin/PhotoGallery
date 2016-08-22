package com.augmentis.ayp.photogallery;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Noppharat on 8/19/2016.
 */
public class PhotoGalleryPreference {
    private static final String TAG = "PhotoGalleryPreference";
    private static final String PREF_SEARCH_KEY = "PhotoGalleryPreference";

    public static String getStoredSearchKey(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString("SEARCH_KEY", null);
    }

    public static void setStoredSearchKey(Context context, String key){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit()
                .putString(PREF_SEARCH_KEY, key)
                .apply();
    }
}
