package com.ankit.audiodemo.utility;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.ankit.audiodemo.models.AudioPlaybackList;
import com.ankit.audiodemo.models.AudioPlaybackModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Time;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import okhttp3.ResponseBody;

public class Utility {

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.anchor.app.PlayNewAudio";
    public static final String Broadcast_PLAY_AUDIO = "com.anchor.app.PlayAudio";
    public static final String Broadcast_PAUSE_AUDIO = "com.anchor.app.PauseAudio";
    public static final String Broadcast_SKIP_NEXT_AUDIO = "com.anchor.app.SkipNextAudio";
    public static final String Broadcast_SKIP_PREVIOUS_AUDIO = "com.anchor.app.SkipPreviousAudio";
    public static final String Broadcast_RESUME_AUDIO = "com.anchor.app.ResumeAudio";
    public static final String Broadcast_STOP_AUDIO = "com.anchor.app.StopAudio";

    public static int getPixelValue(Context context, float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        );
    }

    public static int dpToPx(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    public static String greeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        if (timeOfDay >= 0 && timeOfDay < 12) {
            return "Good Morning";
        } else if (timeOfDay >= 12 && timeOfDay < 16) {
            return "Good Afternoon";
        } else if (timeOfDay >= 16 && timeOfDay < 21) {
            return "Good Evening";
        } else if (timeOfDay >= 21 && timeOfDay < 24) {
            return "Good Night";
        } else {
            return "Hello";
        }
    }

    public static void showError(Context context, ResponseBody responseBody) {
        JSONObject jObjError = null;
        try {
            jObjError = new JSONObject(responseBody.string());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            JSONObject data = jObjError.getJSONObject("errors");
            Iterator<String> errorKeys = data.keys();

            while (errorKeys.hasNext()) {
                String key = errorKeys.next();
                try {
                    if (data.get(key) instanceof JSONArray) {
                        JSONArray array = data.getJSONArray(key);
                        int size = array.length();
                        if (size > 0) {
                            Toast.makeText(context, array.getString(0), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        System.out.println(key + ":" + data.getString(key));
                    }
                } catch (Throwable e) {
                    try {
                        System.out.println(key + ":" + data.getString(key));
                    } catch (Exception ee) {
                    }
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    public static void showDialogOK(Context context, String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }

    public static AudioPlaybackModel getCurrentPlaybackAudio(Context context) {
        try {
            if (PrefUtils.getAudioPlaybackIndex(context) == -1) {
                Log.e("getCurrentPlaybackAudio", "1");
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        if (PrefUtils.getAudioPlayback(context) == null) {
            Log.e("getCurrentPlaybackAudio", "2");
            return null;
        }
        if (PrefUtils.getAudioPlayback(context).playbacks.size() == 0) {
            Log.e("getCurrentPlaybackAudio", "3");
            return null;
        }
        try {
            Log.e("getCurrentPlaybackAudio", "4");
            return PrefUtils.getAudioPlayback(context).playbacks.get(PrefUtils.getAudioPlaybackIndex(context));
        } catch (Exception e) {
            PrefUtils.setAudioPlayback(new AudioPlaybackList(new ArrayList<>()), context);
            PrefUtils.setAudioPlaybackIndex(-1, context);
            return null;
        }
    }

    /**
     * Function to convert milliseconds time to
     * Timer Format
     * Hours:Minutes:Seconds
     */
    public static String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

    public static void tapToPlay(Activity activity, List<AudioPlaybackModel> audioList, int index) {
        Log.e("tapToPlay", "tapToPlay");
        if (audioList != null) {
            if (audioList.size() > 0) {
                Log.e("audioList", audioList.get(0).audioUrl);
                PrefUtils.setAudioPlayback(new AudioPlaybackList(audioList), activity);
                PrefUtils.setAudioPlaybackIndex(index, activity);
            }
        }
        BottomPlaybackControl.getInstance().initViews(1);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e("tapToPlay", "handler");
                activity.sendBroadcast(new Intent(Broadcast_PLAY_NEW_AUDIO));
            }
        }, 300);
    }

    public static String getTime(int hr, int min) {
        Time tme = new Time(hr, min, 0);//seconds by default set to zero
        Format formatter;
        formatter = new SimpleDateFormat("h:mm a");
        return formatter.format(tme);
    }

    public static String getPathFromURI(Context context, Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    public static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "anchor");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("anchor", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    public static File bitmapToFile(Bitmap bitmap) {
        // File name like "image.png"
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String fileNameToSave = sdf.format(new Date());
        //create a file to write bitmap data
        File file = null;
        try {
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + fileNameToSave + ".png");
            file.createNewFile();

            //Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos); // YOU can also save it in JPEG
            byte[] bitmapdata = bos.toByteArray();

            //write the bytes in file
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
            Log.e("exists", file.exists() + "");
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return file; // it will return null
        }
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void showKeyboard(Activity activity, EditText et) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
    }

    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
