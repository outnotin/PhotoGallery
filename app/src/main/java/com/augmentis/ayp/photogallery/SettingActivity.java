package com.augmentis.ayp.photogallery;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

/**
 * Created by Noppharat on 9/5/2016.
 */
public class SettingActivity extends SingleFragmentActivity{

    public static Intent newIntent(Context context){
        return new Intent(context, SettingActivity.class);
    }

    @Override
    protected Fragment onCreateFragment() {
        return PhotoSettingFragment.newInstance();
    }
}
