package com.augmentis.ayp.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by Noppharat on 8/16/2016.
 */
public class FlickrFetcher {

    private static final String TAG = "FlickrFetcher";

    public byte[] getUrlBytes(String urlSpec) throws IOException{
        URL url = new URL(urlSpec);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            //if connection is not OK throw new IOException
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + " : with " + urlSpec);
            }
            int byteRead = 0;

            byte[] buffer = new byte[2048];

            while((byteRead = in.read(buffer)) > 0){
                out.write(buffer, 0, byteRead);
            }

            out.close();

            return out.toByteArray();
        }finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    private static final String FLICKR_URL = "https://api.flickr.com/services/rest/";
    private static final String API_KEY = "9b797212acaa4c434c84d22c09f4d9e9";

    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String METHOD_SEARCH = "flickr.photos.search";
    private static final String METHOD_GET_SIZES = "flickr.photos.getsizes";

    private String buildUrl(String method, String ... param) throws IOException{
        Uri baseUrl = Uri.parse(FLICKR_URL);
        Uri.Builder builder = baseUrl.buildUpon();
        builder.appendQueryParameter("method", method);
        builder.appendQueryParameter("api_key", API_KEY);
        builder.appendQueryParameter("format", "json");
        builder.appendQueryParameter("nojsoncallback", "1");
        builder.appendQueryParameter("extras", "url_s, url_z");

        if(METHOD_SEARCH.equalsIgnoreCase(method)){
            builder.appendQueryParameter("text", param[0]);
        }

        Uri completeUrl = builder.build();
        String url = completeUrl.toString();

        Log.i(TAG, "Run URL : " + url);

        return url;
    }


    private String queryItem(String url) throws IOException{
        Log.i(TAG, "Run URL : " + url);
        String jsonString = getUrlString(url);

        Log.i(TAG, "Search Received JSON: " + jsonString);
        return jsonString;
    }

    /**
     * Search photo then put into <b>items</b>
     * @param items array target
     * @param
     */

    public void searchPhotos(List<GalleryItem> items, String key){
        try{
            String url = buildUrl(METHOD_SEARCH, key);
            String jsonUrl = queryItem(url);
            if(jsonUrl != null){
                parseJSON(items, jsonUrl);
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, "Failed to fetch items", e);
        }
    }



    public void getRecentPhotos(List<GalleryItem> items){
        try {
            String url = buildUrl(METHOD_GET_RECENT);
            String jsonStr = queryItem(url);
            if(jsonStr != null){
                parseJSON(items, jsonStr);
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, "Failed to fetch items", e);
        }
    }

    private void parseJSON(List<GalleryItem> newGalleryItemList, String jsonBodyStr) throws IOException, JSONException{
        JSONObject jsonBody = new JSONObject(jsonBodyStr);
        JSONObject photosJson = jsonBody.getJSONObject("photos");
        JSONArray photoListJson = photosJson.getJSONArray("photo");

        int len = photoListJson.length();

        for(int i = 0 ; i < len ; i++){
            JSONObject jsonPhotoItem = photoListJson.getJSONObject(i);

            GalleryItem item = new GalleryItem();

            item.setId(jsonPhotoItem.getString("id"));
            item.setTitle(jsonPhotoItem.getString("title"));
            item.setOwner(jsonPhotoItem.getString("owner"));

            if(!jsonPhotoItem.has("url_s")){
                continue;
            }

            item.setUrl(jsonPhotoItem.getString("url_s"));

            if(!jsonPhotoItem.has("url_z")){
                continue;
            }

            item.setBigSizeUrl(jsonPhotoItem.getString("url_z"));

            newGalleryItemList.add(item);
        }
    }

}
