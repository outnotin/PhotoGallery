package com.augmentis.ayp.photogallery;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

public class PhotoGalleryActivity extends SingleFragmentActivity {


    @Override
    protected Fragment onCreateFragment() {
        return PhotoGalleryFragment.newInstance();
    }
}
