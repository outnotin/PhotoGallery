package com.augmentis.ayp.photogallery;

import android.net.Uri;

/**
 * Created by Noppharat on 8/16/2016.
 */
public class GalleryItem {
    private String id;
    private String title;
    private String url;
    private String bigSizeUrl;
    private String mOwner;


    public static void printHello(){
        System.out.println("Hello");
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getName(){
        return getTitle();
    }

    public void setName(String name){
        setTitle(name);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof GalleryItem){
            //is GalleryItem too!!
            GalleryItem that = (GalleryItem) obj;
            return that.id != null && id != null && that.id.equals(id);
        }
        return false;
    }

    public void setBigSizeUrl(String bigSizeUrl) {
        this.bigSizeUrl = bigSizeUrl;
    }

    public String getBigSizeUrl() {
        return bigSizeUrl;
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }

    public String getOwner() {
        return mOwner;
    }

    private final String PHOTO_URL_PREFIK = "https://www.flickr.com/photos/";

    public Uri getPhotoUri(){
        return Uri.parse(PHOTO_URL_PREFIK)
                .buildUpon()            //return builder
                .appendPath(mOwner)
                .appendPath(id)
                .build();               //return Uri
    }
}
