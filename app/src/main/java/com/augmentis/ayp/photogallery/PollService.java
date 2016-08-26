package com.augmentis.ayp.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Noppharat on 8/22/2016.
 */
public class PollService extends IntentService {

    private static final String TAG = "PollService";

    private static final int POLL_INTERVAL = 1000 * 60; // 60 seconds

    //public broadcast name for action
    public static final String ACTION_SHOW_NOTIFICATION = "com.augmentis.ayp.photogallery.ACTION_SHOW_NOTIFICATION";
    public static final String PERMISSION_SHOW_NOTIF = "com.augmentis.ayp.photogallery.RECEIVE_SHOW_NOTIFICATION";
    public static final String REQUEST_CODE = "REQUEST_CODE_INTENT";
    public static final String NOTIFICATION = "NOTIF";

    public static Intent newIntent(Context context){
        return new Intent(context, PollService.class);
    }

    public static void setServiceAlarm(Context context, boolean isOn){
        Intent intent = PollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(isOn){

//            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                                          // AlarmManager.RTC -> System.currentTimeMillis();
                alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, // param 1: Mode
                        SystemClock.elapsedRealtime(),                          // param 2: Start
                        POLL_INTERVAL,                                          // param 3: Interval
                        pendingIntent);
//            } else {
//                PollJobService.start(context);
//            }
            // param 4: Pending action(intent)
        }else {
//           if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
               alarmManager.cancel(pendingIntent); //cancel interval call
               pendingIntent.cancel(); //cancel pending intent call
//           }else {
//               PollJobService.stop(context);
//           }
        }
        PhotoGalleryPreference.setStoredIsAlarmOn(context, isOn);
    }

    public static boolean isServiceAlarmOn(Context context){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            Intent intent = PollService.newIntent(context);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
            return pendingIntent != null;
        }else {
            return PollJobService.isRun(context);
        }
    }

    public PollService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG,"Receive a call from intent : " + intent);

        if(!isNetworkAvailableAndConnected()){
            return;
        }

        Log.i(TAG,"Active network");

        String query = PhotoGalleryPreference.getStoredSearchKey(this);
        String storedLastId = PhotoGalleryPreference.getStoredLastId(this);

        List<GalleryItem> galleryItemList = new ArrayList<>();
        FlickrFetcher flickrFetcher = new FlickrFetcher();
        if(query == null){
            flickrFetcher.getRecentPhotos(galleryItemList);
        }else{
            flickrFetcher.searchPhotos(galleryItemList, query);
        }

        if(galleryItemList.size() == 0){
            return;
        }

        Log.i(TAG, "Found search or recent item");

        String newestId = galleryItemList.get(0).getId();//fetching first item

        if(newestId.equals(storedLastId)){
            Log.i(TAG, "No new item");
        }else {
            Log.i(TAG, "New item found");

            Resources res = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
            NotificationCompat.Builder notiBuilder = new NotificationCompat.Builder(this);
            notiBuilder.setTicker(res.getString(R.string.new_picture));
            notiBuilder.setSmallIcon(android.R.drawable.ic_menu_report_image);
            notiBuilder.setContentTitle(res.getString(R.string.new_picture_title));
            notiBuilder.setContentText(res.getString(R.string.new_picture_content));
            notiBuilder.setContentIntent(pi);
            notiBuilder.setAutoCancel(true);

            Notification notification = notiBuilder.build(); // build notification from builder

//            NotificationManagerCompat nm = NotificationManagerCompat.from(this); //get notificationmanager from context
////            nm.notify(newestId.hashCode(), notification);//call notification
//            nm.notify(0, notification);//call notification
////            nm.notify(Long.valueOf(newestId).intValue(), notification);
//            sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PERMISSION_SHOW_NOTIF);
            sendBackgroundNotification(0, notification);
        }

        PhotoGalleryPreference.setStoredLastId(this, newestId);
    }

    private void sendBackgroundNotification(int requestCode, Notification notification){
        Intent intent = new Intent(ACTION_SHOW_NOTIFICATION);
        intent.putExtra(REQUEST_CODE, requestCode);
        intent.putExtra(NOTIFICATION, notification);
        sendOrderedBroadcast(intent, PERMISSION_SHOW_NOTIF, null, null, Activity.RESULT_OK, null, null);
    }

    private boolean isNetworkAvailableAndConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isActiveNetwork = cm.getActiveNetworkInfo() != null;
        boolean isActiveNetworkConnection = isActiveNetwork && cm.getActiveNetworkInfo().isConnected();

        return isActiveNetworkConnection;
    }

}
