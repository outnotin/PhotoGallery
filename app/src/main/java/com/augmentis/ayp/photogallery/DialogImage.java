package com.augmentis.ayp.photogallery;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Noppharat on 8/22/2016.
 */
public class DialogImage extends DialogFragment implements DialogInterface.OnClickListener{

    private String url;
    private loadImageTask mLoadImageTask;

    public static DialogImage newInstance(String photoUrl){
        DialogImage di = new DialogImage();
        Bundle args = new Bundle();
        args.putSerializable("ARG_LARGE_IMAGE", photoUrl);
        di.setArguments(args);
        return di;
    }

    ImageView _ImageView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        url = getArguments().getSerializable("ARG_LARGE_IMAGE").toString();

        Bitmap loadBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.loading_move);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_image, null);
        _ImageView = (ImageView) v.findViewById(R.id.dialog_image_view);
        _ImageView.setImageBitmap(loadBitmap);

//        new Thread(new loadImageRunnable()).start();
        loadBigImage();

        builder.setView(v);
        builder.setPositiveButton(android.R.string.ok, this);
        return builder.create();
    }

    private Bitmap bitmapFromUrl(String url) throws IOException{
        Bitmap bitmap;

        HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
        connection.setRequestProperty("User-agent", "Mozilla/4.0");
        connection.connect();
        InputStream inputStream = connection.getInputStream();
        bitmap = BitmapFactory.decodeStream(inputStream);
        return bitmap;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        getActivity().getFragmentManager().popBackStack();
    }

//    class loadImageRunnable implements Runnable{
//
//        @Override
//        public void run() {
//            loadImage();
//        }
//    }

//    private void loadImage(){
//        try {
//            Bitmap bitmap = bitmapFromUrl(url);
//            _ImageView.setImageBitmap(bitmap);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private void loadBigImage(){
        if(mLoadImageTask == null || !mLoadImageTask.isRunning()){
            mLoadImageTask = new loadImageTask();
        }

        mLoadImageTask.execute();


    }

    class loadImageTask extends AsyncTask<String, Void, Bitmap> {

        boolean running = false;

        boolean isRunning(){
            return running;
        }


        @Override
        protected Bitmap doInBackground(String... strings) {
            synchronized (this){
                running = true;
            }

            try {
                Bitmap bitmap = bitmapFromUrl(url);
                return bitmap;

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                synchronized (this){
                    running = false;
                }
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            _ImageView.setImageBitmap(bitmap);
        }
    }
}
