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
import android.media.MediaMetadataRetriever;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
    private Button contact;
    private Media currentMedia;
    int counter = 0;
    int videoCounter = 0;
    long duration = 10000;
    boolean isFirstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(8);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        contact = findViewById(R.id.contact);
        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);
        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);
        imageIds = new ArrayList<>();
        videoIds = new ArrayList<>();
        oldImageIds = new ArrayList<>();
        oldVideoIds = new ArrayList<>();
        mediaArrayList = new ArrayList<>();
        contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               
            }
        });

        Timer t = new java.util.Timer();
        t.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        if(isFirstTime){
                            isFirstTime = false;
                            play();
                        }
                    }
                },
                10000,10000
        );

        t.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {

                        getImagesFromStorage();
                        getVideosFromStorage();
                    }
                },
                5000,10000
        );

        t.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        BroadcastReceiver receiver = new BroadcastReceiver() {
                            public void onReceive(Context context, Intent intent) {
                                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                                if (plugged == BatteryManager.BATTERY_PLUGGED_AC)
                                {
                                    unLock();

                                }
                                else if (plugged == BatteryManager.BATTERY_PLUGGED_USB)
                                {
                                    unLock();
                                }
                                else if (plugged == 0)
                                {
                                    lock();
                                }
                                else
                                {
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
    }
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(!hasFocus) {
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
        }
    }


    public void play(){

        ArrayList<Media> mediaArrayListCopy = mediaArrayList;
        if (!mediaArrayListCopy.isEmpty())
        {
            Media media = mediaArrayListCopy.get(counter);
            if (media.type == 0)
            {
                Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable myRunnable = new Runnable() {

                    @Override
                    public void run() {
                        imageView.setImageURI(media.mediaUri);
                        videoView.setVisibility(View.INVISIBLE);
                        imageView.setVisibility(View.VISIBLE);
                        new java.util.Timer().schedule(
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        skip();
                                        play();
                                    }
                                },
                                duration
                        );
                    }
                };
                mainHandler.post(myRunnable);
            }
            else if (media.type == 1 && videoCounter > 0)
            {
                Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        imageView.setVisibility(View.INVISIBLE);
                        videoView.setVisibility(View.VISIBLE);
                        videoView.setVideoURI(media.mediaUri);
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(getApplicationContext(),media.mediaUri);
                        long secondDuration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                        retriever.release();

                        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {

                                videoView.start();
                                new java.util.Timer().schedule(
                                        new java.util.TimerTask() {
                                            @Override
                                            public void run() {
                                                skip();
                                                play();
                                            }
                                        },
                                        secondDuration
                                );
                            }
                        });
                    }
                };
                mainHandler.post(myRunnable);
            }else{
                skip();
                play();
            }
        }else{
            play();
        }
    }

    private void skip()
    {
        if(counter + 1 < mediaArrayList.size())
        {
            counter ++;
        }
        else
        {
            counter = 0;
        }
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
        imageIds.clear();
        firebaseFirestore.collection("images")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult())
                            {
                                String name = document.getId();
                                if (!oldImageIds.contains(name)) imageIds.add(name);
                            }
                            for (String x : imageIds )
                            {
                                oldImageIds.add(x);
                                String link = "images/" + x;
                                StorageReference newRef = storageReference.child(link);
                                File localFile = null;
                                try
                                {
                                    localFile = File.createTempFile(x, ".jpg");
                                    imageUri = Uri.fromFile(localFile);
                                    Media newImage = new Media(x,0,imageUri);
                                    DocumentReference documentReference = firebaseFirestore.collection("images").document(newImage.id);
                                    documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            String phoneNumber = documentSnapshot.get("name").toString();
                                            newImage.setPhoneNumber(phoneNumber);
                                            mediaArrayList.add(newImage);
                                        }
                                    });

                                }
                                catch (IOException e)
                                {
                                    e.printStackTrace();
                                }
                                newRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                        System.out.println("Image is downloaded.");
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        System.out.println("Image cannot be downloaded.");
                                    }
                                });
                            }
                        }
                    }
                });


    }

    public void getVideosFromStorage()
    {
        videoIds.clear();
        firebaseFirestore.collection("videos")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult())
                            {
                                String name = document.getId();
                                if(!oldVideoIds.contains(name)) videoIds.add(name);
                            }
                            for (String x : videoIds )
                            {
                                oldVideoIds.add(x);
                                String link = "videos/" + x;
                                StorageReference newRef = storageReference.child(link);
                                File localFile = null;
                                try
                                {
                                    localFile = File.createTempFile(x, ".mp4");
                                    videoUri = Uri.fromFile(localFile);
                                    Media newVideo = new Media(x,1,videoUri);

                                    DocumentReference documentReference = firebaseFirestore.collection("videos").document(newVideo.id);
                                    documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            String phoneNumber = documentSnapshot.get("name").toString();
                                            newVideo.setPhoneNumber(phoneNumber);
                                            mediaArrayList.add(newVideo);

                                        }
                                    });

                                }
                                catch (IOException e)
                                {
                                    e.printStackTrace();
                                }
                                newRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {

                                    @Override
                                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot)
                                    {
                                        System.out.println("Video is downloaded.");
                                        videoCounter ++;
                                    }

                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception)
                                    {
                                        System.out.println("Video cannot be downloaded.");
                                    }

                                });

                            }
                        } else {
                            Log.d("ana","hata");
                        }
                    }
                });
    }

    public void printArray(String location,ArrayList<Media> array)
    {
        if (!array.isEmpty())
        {
            for (int i = 0; i < array.size() ; i++)
            {
                System.out.println(array.get(i).mediaUri+location+"uri");

            }
        }

    }

}
