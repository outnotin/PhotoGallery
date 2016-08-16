package com.augmentis.ayp.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mRecyclerView = (RecyclerView) v.findViewById(R.id.photo_gallery_recyvler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));



        mFlickrFetcher = new FlickrFetcher();
        new FetcherTask().execute();//Run another thread
        return v;
    }

    public class PhotoGalleryHolder extends RecyclerView.ViewHolder{

        private TextView mText;

        public PhotoGalleryHolder(View itemView) {
            super(itemView);
            mText = (TextView) itemView;
        }

        public void bindGalleryItem(GalleryItem galleryItem) {
            mText.setText(galleryItem.getTitle());
        }
    }

    public class PhotoGalleryAdapter extends RecyclerView.Adapter<PhotoGalleryHolder>{

        List<GalleryItem> mGalleryItemList;

        PhotoGalleryAdapter(List<GalleryItem> galleryItems){
            mGalleryItemList = galleryItems;
        }

        @Override
        public PhotoGalleryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getActivity()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new PhotoGalleryHolder(v);
        }

        @Override
        public void onBindViewHolder(PhotoGalleryHolder holder, int position) {
            holder.bindGalleryItem(mGalleryItemList.get(position));
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

        protected void onPostExecute(List<GalleryItem> galleryItem){
            if(mAdapter == null){
                mAdapter = new PhotoGalleryAdapter(galleryItem);
                mRecyclerView.setAdapter(mAdapter);
            }
        }

    }
}
