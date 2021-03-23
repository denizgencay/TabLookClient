package com.example.tablookuser;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    private FirebaseStorage firebaseStorage;
    private FirebaseFirestore firebaseFirestore;
    private StorageReference storageReference;
    private ImageView imageView;
    private VideoView videoView;
    private Uri imageUri, videoUri;
    private ArrayList<String> imageIds, videoIds, oldImageIds, oldVideoIds;
    private ArrayList<Media> mediaArrayList;
    private Timer timer = new Timer();
    boolean isImageShowing = false;
    int counter = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);
        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);
        imageIds = new ArrayList<>();
        videoIds = new ArrayList<>();
        oldImageIds = new ArrayList<>();
        oldVideoIds = new ArrayList<>();
        mediaArrayList = new ArrayList<>();

        Timer t = new java.util.Timer();
        t.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        getImagesFromStorage();
                        getVideosFromStorage();
                    }
                },
                0,5000
        );
        t.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        BroadcastReceiver receiver = new BroadcastReceiver() {
                            public void onReceive(Context context, Intent intent) {
                                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                                if (plugged == BatteryManager.BATTERY_PLUGGED_AC) {
                                    System.out.println("babam1");
                                    unLock();

                                } else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {

                                    unLock();

                                } else if (plugged == 0) {
                                    System.out.println("babam2");
                                    lock();

                                } else {

                                    Toast.makeText(getApplicationContext(),"Non",Toast.LENGTH_LONG).show();
                                }
                            }
                        };
                        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                        registerReceiver(receiver, filter);
                    }
                },
                0,10000
        );
        if (!videoView.isPlaying()) play();
    }

    private void unLock(){
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock screenLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, getLocalClassName());
        screenLock.acquire();

        if (screenLock.isHeld()) {
            screenLock.release();
        }
        
        KeyguardManager mKeyGuardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (mKeyGuardManager.inKeyguardRestrictedInputMode()) {
            KeyguardManager.KeyguardLock keyguardLock = mKeyGuardManager.newKeyguardLock(getLocalClassName());
            keyguardLock.disableKeyguard();
        }
    }


    private void lock() {
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (pm.isInteractive()) {
            DevicePolicyManager policy = (DevicePolicyManager)
                    getSystemService(Context.DEVICE_POLICY_SERVICE);
            try {
                policy.lockNow();
            } catch (SecurityException ex) {
                Toast.makeText(
                        this,
                        "Yönetici yetkisi vermelisin",
                        Toast.LENGTH_LONG).show();
                ComponentName admin = new ComponentName(getApplicationContext(), AdminReceiver.class);
                Intent intent = new Intent(
                        DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(
                        DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                        this.startActivity(intent);
            }
        }
    }

    private void getImagesFromStorage()
    {
        firebaseFirestore.collection("images")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String name = document.getId();
                                imageIds.add(name);
                            }
                            for (String x : imageIds ) {
                                if (!oldImageIds.contains(x))
                                {
                                    String link = "images/" + x;
                                    StorageReference newRef = storageReference.child(link);

                                    File localFile = null;
                                    try
                                    {
                                        localFile = File.createTempFile(x, ".jpg");
                                        imageUri = Uri.fromFile(localFile);
                                        System.out.println(localFile.toString() + "buaraya");
                                    }
                                    catch (IOException e)
                                    {
                                        e.printStackTrace();
                                    }
                                    newRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            System.out.println("Image is downloaded.");
                                            Media newImage = new Media(x,0,imageUri);
                                            mediaArrayList.add(newImage);
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            System.out.println("Image cannot be downloaded.");
                                        }
                                    });
                                }
                            }
                            System.out.println(imageIds + "imageIdArray");
                            if (!oldImageIds.containsAll(imageIds))
                            {
                                for (String newS : imageIds) {
                                    if (!oldImageIds.contains(newS))
                                    {
                                        oldImageIds.add(newS);
                                    }
                                }

                            }
                            imageIds.clear();
                            System.out.println(oldImageIds + "oldImagesArray");
                        } else {
                            Log.d("ana","hata");
                        }
                    }
                });


    }

    public void getVideosFromStorage()
    {
        firebaseFirestore.collection("videos")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String name = document.getId();
                                videoIds.add(name);
                            }
                            for (String x : videoIds ) {
                                if (!oldVideoIds.contains(x))
                                {
                                    String link = "videos/" + x;
                                    StorageReference newRef = storageReference.child(link);

                                    File localFile = null;
                                    try {
                                        localFile = File.createTempFile(x, ".mp4");
                                        videoUri = Uri.fromFile(localFile);
                                        System.out.println(localFile.toString() + "buaraya");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    newRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            System.out.println("Video is downloaded.");
                                            Media newImage = new Media(x,1,videoUri);
                                            mediaArrayList.add(newImage);
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            System.out.println("Video cannot be downloaded.");
                                        }
                                    });
                                }
                            }
                            System.out.println(videoIds + "videoIdArray");
                            if (!oldVideoIds.containsAll(videoIds))
                            {
                                for (String newV : videoIds) {
                                    if (!oldVideoIds.contains(newV))
                                    {
                                        oldVideoIds.add(newV);
                                    }
                                }

                            }
                            videoIds.clear();
                            System.out.println(oldVideoIds + "oldVideosArray");
                        } else {
                            Log.d("ana","hata");
                        }
                    }
                });
    }
    public void play(){

        ArrayList<Media> mediaArrayListCopy = mediaArrayList;


        new CountDownTimer(5000, 1000) { // 5000 = 5 sec

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                System.out.println("burdaFinish");

                Media media = mediaArrayListCopy.get(counter);
                if (media.type == 0)
                {
                    videoView.setVisibility(View.INVISIBLE);
                    Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            imageView.setVisibility(View.VISIBLE);
                            imageView.setImageURI(media.mediaUri);
                            System.out.println("burdaSetImage");
                            skip();
                            play();

                        }
                    };
                    mainHandler.post(myRunnable);

                }
                else if (media.type == 1)
                {
                    videoView.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.INVISIBLE);
                    Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            videoView.setVideoURI(media.mediaUri);

                            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    mp.setLooping(true);
                                    //videoView.setVisibility(View.VISIBLE);
                                    System.out.println("burdaVideo");
                                    imageView.setVisibility(View.INVISIBLE);
                                    videoView.start();
                                    skip();
                                    play();
                                }
                            });
                        }
                    };
                    mainHandler.post(myRunnable);
                }
            }
        }.start();
    }
    private void skip(){
        if(counter + 1 < mediaArrayList.size()){
            counter ++;
        }else{
            counter = 0;
        }
    }


    private void showMedia() {
        if(!mediaArrayList.equals(null))
        {

            System.out.println(mediaArrayList.size()+ "Size:");
            ArrayList<Media> mediaArrayListCopy = mediaArrayList;
            for (int i = 0 ; i < mediaArrayListCopy.size() ; i++)
            {
                Media media = mediaArrayListCopy.get(i);
                if (media.type == 0)
                {
                    Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageURI(media.mediaUri);
                            isImageShowing = true;

                        }
                    };
                    mainHandler.post(myRunnable);

                    break;
                }
                else if (media.type == 1)
                {
                    Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            videoView.setVideoURI(media.mediaUri);
                            System.out.println("geldiVideo");
                            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    mp.setLooping(true);
                                    videoView.start();
                                }
                            });
                        }
                    };
                    mainHandler.post(myRunnable);
                }
            }
//            if(isImageShowing == true){
//                try {
//                    TimeUnit.SECONDS.sleep(10000);
//                    System.out.println("geldiSleep");
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("geldi");
//                showMedia();
//            }
        }

    }
}
