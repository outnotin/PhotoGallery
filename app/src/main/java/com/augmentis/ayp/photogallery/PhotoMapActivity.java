package com.augmentis.ayp.photogallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.support.v4.app.Fragment;

/**
 * Created by Noppharat on 9/5/2016.
 */
public class PhotoMapActivity extends SingleFragmentActivity {

    private static final String KEY_LOCATION = "KEY_LOCATION";
    private static final String KEY_GALLERY_ITEM_LOC = "KEY_GALLERY_ITEM_LOC";
    private static final String KEY_BITMAP = "KEY_BITMAP";

    protected static Intent newIntent(Context context, Location location, Location galleryItemLoc, String url){
        Intent intent = new Intent(context, PhotoMapActivity.class);
        intent.putExtra(KEY_LOCATION, location);
        intent.putExtra(KEY_GALLERY_ITEM_LOC, galleryItemLoc);
        intent.putExtra(KEY_BITMAP, url);

        return intent;
    }

    @Override
    protected Fragment onCreateFragment() {


        if(getIntent() != null){
            Location location = getIntent().getParcelableExtra(KEY_LOCATION);
            Location galleryItemLoc = getIntent().getParcelableExtra(KEY_GALLERY_ITEM_LOC);
            String url = getIntent().getStringExtra(KEY_BITMAP);
            return PhotoMapFragment.newInstance(location, galleryItemLoc, url);
        }
        return PhotoMapFragment.newInstance();
    }
}
