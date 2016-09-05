package com.augmentis.ayp.photogallery;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.LruCache;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Noppharat on 8/16/2016.
 */
public class PhotoGalleryFragment extends VisibleFragment {
    private static final String TAG = "PhotoGalleryFragment";
    private static final String DIALOG_IMAGE = "DialogImage";
    private static final int REQUEST_PERMISSION_LOCATION = 1233;

    public static PhotoGalleryFragment newInstance() {

        Bundle args = new Bundle();

        PhotoGalleryFragment fragment = new PhotoGalleryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private RecyclerView mRecyclerView;
    private List<GalleryItem> mItem;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloaderThread;
    private FetcherTask mFetcherTask;
    private String mSearchKey;
    private String photoUrl;
    private Boolean mUseGPS;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;


    private GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
//            mUseGPS = PhotoGalleryPreference.getUseGPS(getActivity());
            if(mUseGPS){
                findLocation();
            }
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    };

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;

            Log.d(TAG, "onLocationChanged: " + location.getLatitude() + " , " + location.getLongitude());
            Toast.makeText(getActivity(), location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_LONG).show();

        }
    };

    private LruCache<String, Bitmap> mMemoryCache;
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    final int cacheSize = maxMemory;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        setHasOptionsMenu(true);

        mUseGPS = PhotoGalleryPreference.getUseGPS(getActivity());
        mSearchKey = PhotoGalleryPreference.getStoredSearchKey(getActivity());

//        Log.d(TAG, "Start intent service");
        Intent i = PollService.newIntent(getActivity());
        getActivity().startService(i);
        PollService.setServiceAlarm(getContext(), true);

        Log.d(TAG, "Memory size = " + maxMemory + " K");
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };

        Handler responseUIHandler = new Handler();

        ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder> listener =
                new ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail, String url) {
                if (null == mMemoryCache.get(url)) {
                    mMemoryCache.put(url, thumbnail);
                }
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                target.bindDrawable(drawable);
            }
        };

        mThumbnailDownloaderThread = new ThumbnailDownloader<>(responseUIHandler);
        mThumbnailDownloaderThread.setThumbnailDownloadListener(listener);
        mThumbnailDownloaderThread.start();
        mThumbnailDownloaderThread.getLooper();

        Log.i(TAG, "Start background thread");

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .build();

//        PollJobService.start(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloaderThread.quit();
        Log.i(TAG, "Stop background thread");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloaderThread.clearQueue();
    }

    @Override
    public void onPause() {
        super.onPause();
        PhotoGalleryPreference.setStoredSearchKey(getActivity(), mSearchKey);
        if(mGoogleApiClient.isConnected()){
            unFindLocation();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        String searchKey = PhotoGalleryPreference.getStoredSearchKey(getActivity());
        if(searchKey != null){
            mSearchKey = searchKey;
        }

        mUseGPS = PhotoGalleryPreference.getUseGPS(getActivity());
//
//        Log.d(TAG, "On resume completed, mSearchKey = " + mSearchKey + ", mUseGPS = " + mUseGPS);
//        if(mUseGPS){
//            findLocation();
//        }
    }

    private void findLocation(){
        if(hasPermission()){
            requestLocation();
        }
    }

    private Boolean hasPermission(){
        int premissionStatus = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION);
        if(premissionStatus == PackageManager.PERMISSION_GRANTED){
            return true;
        }

        requestPermissions(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}
                , REQUEST_PERMISSION_LOCATION);

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if(requestCode == REQUEST_PERMISSION_LOCATION){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                requestLocation();
            }
        }
    }

    @SuppressWarnings("all")
    private void requestLocation(){

        if(GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity()) == ConnectionResult.SUCCESS){
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setNumUpdates(50);
            locationRequest.setInterval(1000);

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, mLocationListener);
        }

    }

    private void unFindLocation(){
        if(GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity()) == ConnectionResult.SUCCESS){
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem menuItem = menu.findItem(R.id.mnu_search);
        final SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQuery(mSearchKey, false);//
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Query text submitted: " + query);
                mSearchKey = query;
                loadPhoto();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "Query text changing: " + newText);
                return false;
            }


        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchView.setQuery(mSearchKey, false);
            }
        });

        //render polling
        MenuItem mnuPolling = menu.findItem(R.id.mnu_toggle_polling);
        if(PollService.isServiceAlarmOn(getActivity())){
            mnuPolling.setTitle(R.string.stop_polling);
        }else {
            mnuPolling.setTitle(R.string.start_polling);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.mnu_reload:
                loadPhoto();
                return true;
            case R.id.mnu_toggle_polling:
//                Log.d(TAG, "Start/Stop intent service");
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                Log.d(TAG, ((shouldStartAlarm) ? "Start" : "Stop") + " Intent service");
                PollService.setServiceAlarm(getContext(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu(); // refresh menu
                return true;
            case R.id.mnu_clear_search:
                mSearchKey = null;
                loadPhoto();
                return true;
            case R.id.mnu_manual_check:
                Intent pollIntent = PollService.newIntent(getActivity());
                getActivity().startService(pollIntent);
                return true;
            case R.id.mnu_setting:
                Intent settingIntent = SettingActivity.newIntent(getActivity());
                startActivity(settingIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mRecyclerView = (RecyclerView) v.findViewById(R.id.photo_gallery_recycler_view);
        Resources r = getResources();
        int gridSize = r.getInteger(R.integer.gridSize);

        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), gridSize));
        mItem = new ArrayList<>();
        mRecyclerView.setAdapter(new PhotoGalleryAdapter(mItem));

        mSearchKey = PhotoGalleryPreference.getStoredSearchKey(getActivity());
        loadPhoto();

        Log.d(TAG, "On create complete - Loaded search key = " + mSearchKey);
        return v;
    }

    private void loadPhoto(){
        if(mFetcherTask == null || !mFetcherTask.isRunning()){
            mFetcherTask = new FetcherTask();

            if(mSearchKey != null){
                mFetcherTask.execute(mSearchKey);
            }else{
                mFetcherTask.execute();
            }
        }
    }


    class PhotoHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener{

        private ImageView mPhoto;
//        private String mUrl;
        GalleryItem mGalleryItem;


        public PhotoHolder(View itemView) {
            super(itemView);
            mPhoto = (ImageView) itemView.findViewById(R.id.image_photo);
            mPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, " holder onclick");
                    FragmentManager fm = getFragmentManager();
                    DialogImage di = DialogImage.newInstance(mGalleryItem.getBigSizeUrl());
                    di.show(fm, DIALOG_IMAGE);
                }
            });

            itemView.setOnCreateContextMenuListener(this);
        }

        public void bindDrawable(@NonNull Drawable drawable) {
            mPhoto.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem){
            mGalleryItem = galleryItem;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.setHeaderTitle(mGalleryItem.getBigSizeUrl());

            MenuItem menuItem = menu.add(0, 1, 0, R.string.open_with_external_browser);
            menuItem.setOnMenuItemClickListener(this);
            MenuItem menuItem1 = menu.add(0, 2, 0, R.string.open_in_app_browser);
            menuItem1.setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
//            Toast.makeText(getActivity(),"Open by url : " + mGalleryItem.getBigSizeUrl() ,Toast.LENGTH_SHORT).show();

            switch (item.getItemId()){
                case 1:
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, mGalleryItem.getPhotoUri());
                    startActivity(browserIntent);// call external browser by implicit intent
                    return true;
                case 2:
                    Intent internalIntent = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoUri());
                    startActivity(internalIntent);// call internal activity by explicit intent
                    return true;
                default:
            }

            return false;
        }

//        @Override
//        public void onClick(View view) {
//            Log.d(TAG, " holder onclick");
//            FragmentManager fm = getFragmentManager();
//            DialogImage di = DialogImage.newInstance(photoUrl);
//            di.show(fm, DIALOG_IMAGE);
//
//
//        }
    }

    class PhotoGalleryAdapter extends RecyclerView.Adapter<PhotoHolder>{

        List<GalleryItem> mGalleryItemList;


        PhotoGalleryAdapter(List<GalleryItem> galleryItems) {
            mGalleryItemList = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getActivity()).inflate(
                    R.layout.item_photo, parent, false);
//            v.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    Log.d(TAG, " holder onclick");
//                    FragmentManager fm = getFragmentManager();
//                    DialogImage di = DialogImage.newInstance(photoUrl);
//                    di.show(fm, DIALOG_IMAGE);
//                }
//            });
            return new PhotoHolder(v);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position)  {
//            holder.bindDrawable(mGalleryItemList.get(position));
            Drawable loadDrawable =
                    ResourcesCompat.getDrawable(getResources(), R.drawable.loading_move, null);

            GalleryItem galleryItem = mGalleryItemList.get(position);
//            Log.d(TAG, "bind position : " + position + ", url : " + galleryItem.getUrl());

            holder.bindDrawable(loadDrawable);
            holder.bindGalleryItem(galleryItem);


            if (mMemoryCache.get(galleryItem.getUrl()) != null) {
                Bitmap bitmap = mMemoryCache.get(galleryItem.getUrl());
                holder.bindDrawable(new BitmapDrawable(getResources(), bitmap));
            } else {
                mThumbnailDownloaderThread.queueThumbnailDownload(holder, galleryItem.getUrl());
            }
        }

        @Override
        public int getItemCount() {
            return mGalleryItemList.size();
        }
    }

    class FetcherTask extends AsyncTask<String, Void, List<GalleryItem>>{

        boolean running = false;

        @Override
        protected List<GalleryItem> doInBackground(String ... params) {
            synchronized (this){
                running = true;
            }

            try {
                Log.d(TAG, "Start fercher task");
                List<GalleryItem> itemList = new ArrayList<>();
                FlickrFetcher flickrFetcher = new FlickrFetcher();
                if(params.length > 0){
                    if(mUseGPS && mLocation != null){
                        flickrFetcher.searchPhotos(itemList, params[0],
                                String.valueOf(mLocation.getLatitude()),
                                String.valueOf(mLocation.getLongitude())
                        );
                    }else {
                        flickrFetcher.searchPhotos(itemList, params[0]);

                    }
                }else{
                    flickrFetcher.getRecentPhotos(itemList);
                }
                Log.d(TAG, "Fetcher task finished");
                return itemList;
            }finally {
                synchronized (this){
                    running = false;
                }
            }
        }

        boolean isRunning(){
            return running;
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryTtems) {
            mItem = galleryTtems;
            mRecyclerView.setAdapter(new PhotoGalleryAdapter(mItem));

            String formatString = getResources().getString(R.string.photo_progress_loaded);
            Snackbar.make(mRecyclerView, formatString, Snackbar.LENGTH_SHORT).show();
        }

    }


}



