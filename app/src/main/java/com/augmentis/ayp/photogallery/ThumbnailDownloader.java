package com.augmentis.ayp.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Noppharat on 8/18/2016.
 */
public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";

    private static final int DOWNLOAD_FILE = 2016;

    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestUrlMap = new ConcurrentHashMap<>();

    private Handler mResponseHandler;

    private ThumbnailDownloaderListener<T> mThumbnailDownloaderListener;

    interface ThumbnailDownloaderListener<T>{
        void onThumbnailDownloaded(T target, Bitmap thumbnail, String url);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloaderListener<T> thumbnailDownloaderListener){
        mThumbnailDownloaderListener = thumbnailDownloaderListener;
    }

    public ThumbnailDownloader(Handler mUIHandler) {
        super(TAG);
        mResponseHandler = mUIHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                //Work in the queue
                if(msg.what == DOWNLOAD_FILE){
                    T target = (T) msg.obj;
                    String url = mRequestUrlMap.get(target);
                    Log.i(TAG, "Got message from queue please download this URL: " + url);

                    handlerRequestDownload(target, url);
                }
            }
        };
    }

    public void clearQueue(){
        mRequestHandler.removeMessages(DOWNLOAD_FILE);
    }

    private void handlerRequestDownload(final T target,final String url){
        try {
            if(url == null){
                return;
            }

            byte[] bitMapBytes = new FlickrFetcher().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitMapBytes, 0, bitMapBytes.length);

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    String currentUrl = mRequestUrlMap.get(target);
                    if (currentUrl != null && !currentUrl.equals(url)) {
                        return;
                    }
                    //url is ok(the same one)
                    mRequestUrlMap.remove(target);
                    mThumbnailDownloaderListener.onThumbnailDownloaded(target, bitmap, url);
                }
            });

            Log.i(TAG, "BitMap url downloaded : ");
        }catch (IOException ioe){
            Log.e(TAG, "Error doenloading", ioe);
        }
    }

    public void queueThumbnailDownload(T target, String url){
        Log.i(TAG, "Got url " + url);

        if(null == url){
            mRequestUrlMap.remove(target);
        }else{
            mRequestUrlMap.put(target, url);

            Message msg = mRequestHandler.obtainMessage(DOWNLOAD_FILE, target); //get message from handler
            msg.sendToTarget(); //send to handler
        }
    }
}
