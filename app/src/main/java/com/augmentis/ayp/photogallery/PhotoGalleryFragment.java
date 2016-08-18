package com.augmentis.ayp.photogallery;

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
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Noppharat on 8/16/2016.
 */
public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";
    public static PhotoGalleryFragment newInstance() {

        Bundle args = new Bundle();

        PhotoGalleryFragment fragment = new PhotoGalleryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private RecyclerView mRecyclerView;
    private FlickrFetcher mFlickrFetcher;
    private PhotoGalleryAdapter mAdapter;
    private List<GalleryItem> mItem;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloaderThread;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mFlickrFetcher = new FlickrFetcher();
        new FetcherTask().execute();

        Handler responseUIHandler = new Handler();

        ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder> listener = new ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                target.bindDrawable(drawable);
            }
        };

        mThumbnailDownloaderThread = new ThumbnailDownloader<>(responseUIHandler);
        mThumbnailDownloaderThread.setThumbnailDownloadListener(listener);
        mThumbnailDownloaderThread.start();
        mThumbnailDownloaderThread.getLooper();

        Log.i(TAG,"Start background thread");
    }

    @Override
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


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mRecyclerView = (RecyclerView) v.findViewById(R.id.photo_gallery_recycler_view);
        Resources r = getResources();
        int gridSize  = r.getInteger(R.integer.gridSize);

        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),gridSize));
        mItem = new ArrayList<>();
        mRecyclerView.setAdapter(new PhotoGalleryAdapter(mItem));

//        mFlickrFetcher = new FlickrFetcher();
//        new FetcherTask().execute();//Run another thread
        return v;
    }

    public class PhotoHolder extends RecyclerView.ViewHolder{

        private ImageView mPhoto;

        public PhotoHolder(View itemView) {
            super(itemView);
            mPhoto = (ImageView) itemView.findViewById(R.id.image_photo);
        }

        public void bindDrawable(@NonNull Drawable drawable) {
            mPhoto.setImageDrawable(drawable);
        }
    }

    public class PhotoGalleryAdapter extends RecyclerView.Adapter<PhotoHolder>{

        List<GalleryItem> mGalleryItemList;

        PhotoGalleryAdapter(List<GalleryItem> galleryItems){
            mGalleryItemList = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getActivity()).inflate(R.layout.item_photo, parent, false);
            return new PhotoHolder(v);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
//            holder.bindDrawable(mGalleryItemList.get(position));
            Drawable loadDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.loading_move, null);

            GalleryItem galleryItem = mGalleryItemList.get(position);
            Log.d(TAG, "bind position : " + position + ", url : " + galleryItem.getUrl());

            holder.bindDrawable(loadDrawable);

            mThumbnailDownloaderThread.queueThumbnailDownload(holder, galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItemList.size();
        }
    }

    class FetcherTask extends AsyncTask<Void, Void, List<GalleryItem>>{

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            List<GalleryItem> itemList = new ArrayList<>();
            mFlickrFetcher.fetchItem(itemList);
            return itemList;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            String formatString = getResources().getString(R.string.photo_progress_loaded);
            Snackbar.make(mRecyclerView, formatString, Snackbar.LENGTH_SHORT).show();
        }

        protected void onPostExecute(List<GalleryItem> galleryItem){
            if(mAdapter == null){
                mAdapter = new PhotoGalleryAdapter(galleryItem);
                mRecyclerView.setAdapter(mAdapter);
            }
        }

    }
}
