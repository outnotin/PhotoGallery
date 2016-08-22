package com.augmentis.ayp.photogallery;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.LruCache;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Noppharat on 8/16/2016.
 */
public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";
    private static final String DIALOG_IMAGE = "DialogImage";

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

    private LruCache<String, Bitmap> mMemoryCache;
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    final int cacheSize = maxMemory;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

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
    }

    @Override
    public void onResume() {
        super.onResume();
        String searchKey = PhotoGalleryPreference.getStoredSearchKey(getActivity());
        if(searchKey != null){
            mSearchKey = searchKey;
        }

        Log.d(TAG, "On resume completed");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem menuItem = menu.findItem(R.id.mnu_search);
        final SearchView searchView = (SearchView) menuItem.getActionView();
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

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.mnu_reload:
                loadPhoto();
                return true;
            case R.id.mnu_clear_search:
                mSearchKey = null;
                loadPhoto();
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


    class PhotoHolder extends RecyclerView.ViewHolder{

        private ImageView mPhoto;
        private String mUrl;


        public PhotoHolder(View itemView) {
            super(itemView);
            mPhoto = (ImageView) itemView.findViewById(R.id.image_photo);
            mPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, " holder onclick");
                    FragmentManager fm = getFragmentManager();
                    DialogImage di = DialogImage.newInstance(mUrl);
                    di.show(fm, DIALOG_IMAGE);
                }
            });
        }

        public void bindDrawable(@NonNull Drawable drawable) {
            mPhoto.setImageDrawable(drawable);
        }

        public void setUrl(String _photoUrl){
            mUrl = _photoUrl;
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
            Log.d(TAG, "bind position : " + position + ", url : " + galleryItem.getUrl());

            holder.bindDrawable(loadDrawable);
            holder.setUrl(galleryItem.getBigSizeUrl());


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
                    flickrFetcher.searchPhotos(itemList, params[0]);
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



