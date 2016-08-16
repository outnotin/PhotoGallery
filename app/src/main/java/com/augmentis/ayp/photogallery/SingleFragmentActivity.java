package com.augmentis.ayp.photogallery;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Noppharat on 8/16/2016.
 */
public abstract class SingleFragmentActivity extends AppCompatActivity{
    @LayoutRes
    protected int getLayoutResId(){
        return
                R.layout.activity_single_fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //create container and put fragment
        //CrimeListActivity is responsed in create fragment
        setContentView(getLayoutResId());


        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentById(R.id.fragment_container);

        if(f == null){
            f = onCreateFragment();
            fm.beginTransaction()
                    .add(R.id.fragment_container,f)
                    .commit();

        }else{

        }
    }

    protected abstract Fragment onCreateFragment();
}
