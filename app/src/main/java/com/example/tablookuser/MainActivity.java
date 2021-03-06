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
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    private FirebaseStorage firebaseStorage;
    private FirebaseFirestore firebaseFirestore;
    private StorageReference storageReference;
    private ImageView imageView, contact, contactInfo;
    private VideoView videoView;
    private Uri imageUri, videoUri;
    private ArrayList<String> imageIds, videoIds, oldImageIds, oldVideoIds,databaseImageArrayList,databaseVideoArrayList;
    private ArrayList<Media> mediaArrayList;
    private Media currentMedia;
    private TextView contactInfoText;


    int counter = 0;
    int videoCounter = 0;
    long duration = 60000;
    boolean isFirstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(8);
        setContentView(R.layout.activity_main);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE;
        decorView.setSystemUiVisibility(uiOptions);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        contactInfoText = findViewById(R.id.contactInfoText);
        contactInfo = findViewById(R.id.contactInfo);
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        contact = findViewById(R.id.contact);
        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);
        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);
        databaseImageArrayList = new ArrayList<>();
        databaseVideoArrayList = new ArrayList<>();
        imageIds = new ArrayList<>();
        videoIds = new ArrayList<>();
        oldImageIds = new ArrayList<>();
        oldVideoIds = new ArrayList<>();
        mediaArrayList = new ArrayList<>();
        contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(currentMedia != null){

                   if(currentMedia.type == 0){
                       DocumentReference documentReference = firebaseFirestore.collection("images").document(currentMedia.id);
                       contactInfoText.setText(currentMedia.phoneNumber);
                       contact.setEnabled(false);
                       contact.setVisibility(View.INVISIBLE);
                       contactInfoText.setVisibility(View.VISIBLE);
                       contactInfo.setVisibility(View.VISIBLE);

                   }else{
                       DocumentReference documentReference = firebaseFirestore.collection("videos").document(currentMedia.id);
                       documentReference.update("counter", FieldValue.increment(1));
                       contactInfoText.setText(currentMedia.phoneNumber);
                       contact.setEnabled(false);
                       contact.setVisibility(View.INVISIBLE);
                       contactInfoText.setVisibility(View.VISIBLE);
                       contactInfo.setVisibility(View.VISIBLE);
                   }
               }
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
                        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                        Runnable myRunnable = new Runnable() {
                            @Override
                            public void run() {
                                View decorView = getWindow().getDecorView();
                                int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE;
                                decorView.setSystemUiVisibility(uiOptions);
                            }
                        };
                        mainHandler.post(myRunnable);       //To control views on main thread
                    }
                },
                5000,5000
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
                            public void onReceive(Context context, Intent intent) {                                             //Controling the power
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


    public void play(){         //Setting media to imageView and videoView

        ArrayList<Media> mediaArrayListCopy = mediaArrayList;
        if (!mediaArrayListCopy.isEmpty())
        {
            Media media = mediaArrayListCopy.get(counter);
            String path = media.mediaUri.getPath();
            File localFile = new File(path);
            if(localFile.isFile()){
                System.out.println("fileEntered");
                currentMedia = media;
                System.out.println(currentMedia.mediaUri + "currentMediaUri");
                if (media.type == 0)        //Media type is image
                {
                    Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {

                            imageView.setImageURI(media.mediaUri);
                            videoView.setVisibility(View.INVISIBLE);
                            imageView.setVisibility(View.VISIBLE);
                            contactInfoText.setVisibility(View.INVISIBLE);
                            contactInfo.setVisibility(View.INVISIBLE);
                            contact.setEnabled(true);
                            contact.setVisibility(View.VISIBLE);

                            new java.util.Timer().schedule(
                                    new java.util.TimerTask() {
                                        @Override
                                        public void run() {
                                            skip();     // Chancing mediaArrayList index for getting other midea file
                                            play();     // The function runs recursively for the slideView
                                        }
                                    },
                                    duration
                            );
                        }
                    };
                    mainHandler.post(myRunnable);       //To control views on main thread
                }
                else if (media.type == 1 && videoCounter > 0)
                {
                    Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            String path = media.mediaUri.getPath();
                            File localFile = new File(path);
                            if(localFile.isFile()){
                                imageView.setVisibility(View.INVISIBLE);//To show videoView
                                videoView.setVisibility(View.VISIBLE);      //To show videoView
                                videoView.setVideoURI(media.mediaUri);      //Giving videoView to video path in internal storage.
                                contactInfo.setVisibility(View.INVISIBLE);
                                contactInfoText.setVisibility(View.INVISIBLE);
                                contact.setEnabled(true);
                                contact.setVisibility(View.VISIBLE);              //To get add statistics
                                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                retriever.setDataSource(getApplicationContext(),media.mediaUri);
                                long secondDuration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)); //It takes the video time to show the video image
                                retriever.release();
                                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                    @Override
                                    public void onPrepared(MediaPlayer mp) {
                                        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                                            @Override
                                            public boolean onError(MediaPlayer mp, int what, int extra) {
                                                Log.d("videoNotPlaying", "setOnErrorListener ");
                                                skip();
                                                play();
                                                return true;
                                            }
                                        });
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
                            }else{
                                skip();
                                play();
                            }


                        }
                    };
                    mainHandler.post(myRunnable);
                }else{
                    skip();
                    play();
                }
            }else{
                System.out.println("fileEnteredDosent");
                skip();
                play();
            }
        }else{
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                            Runnable myRunnable = new Runnable() {
                                @Override
                                public void run() {

                                    imageView.setVisibility(View.INVISIBLE);
                                    videoView.setVisibility(View.INVISIBLE);

                                }
                            };
                            mainHandler.post(myRunnable);
                            play();
                        }
                    },
                    5000
            );
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

    private void unLock(){      // Unlocks screen when the power is on
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

    private void lock() {      // Locks screen when the power is off
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (pm.isInteractive()) {
            DevicePolicyManager policy = (DevicePolicyManager)
                    getSystemService(Context.DEVICE_POLICY_SERVICE);
            try {
                policy.lockNow();
            } catch (SecurityException ex) {
                Toast.makeText(
                        this,
                        "Y??netici yetkisi vermelisiniz",
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
        databaseImageArrayList.clear();
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
                                databaseImageArrayList.add(name);
                                if (!oldImageIds.contains(name)) imageIds.add(name);
                                System.out.println(oldImageIds + "oldImageIds");
                            }
                            for (String x : imageIds )
                            {
                                oldImageIds.add(x);
                                String path = x + ".jpg";
                                String link = "images/" + x;
                                StorageReference newRef = storageReference.child(link);
                                File localFile = new File(getFilesDir(),path);          //Creating file directory in internal storage
                                imageUri = Uri.fromFile(localFile);
                                System.out.println(imageUri + "localFileImageUri");
                                Media newImage = new Media(x,0,imageUri);
                                if (!localFile.exists()){
                                    System.out.println("iYok");
                                    DocumentReference documentReference = firebaseFirestore.collection("images").document(newImage.id);
                                    documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            if(documentSnapshot.get("phoneNumber") != null ){
                                                String phoneNumber = documentSnapshot.get("phoneNumber").toString();
                                                newImage.setPhoneNumber(phoneNumber);
                                                mediaArrayList.add(newImage);
                                            }else{
                                                String phone = "No phone number";
                                                newImage.setPhoneNumber(phone);
                                                mediaArrayList.add(newImage);
                                            }

                                        }
                                    });
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
                                }else{
                                    System.out.println("iVAr");
                                    DocumentReference documentReference = firebaseFirestore.collection("images").document(newImage.id);
                                    documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            if(documentSnapshot.get("phoneNumber") != null){
                                                String phoneNumber = documentSnapshot.get("phoneNumber").toString();
                                                newImage.setPhoneNumber(phoneNumber);
                                                mediaArrayList.add(newImage);
                                            }else{
                                                String phone = "No phone number";
                                                newImage.setPhoneNumber(phone);
                                                mediaArrayList.add(newImage);
                                            }

                                        }
                                    });
                                }
                            }

                            if(!mediaArrayList.isEmpty()){
                                if(databaseImageArrayList.isEmpty()){
                                    for(int x = 0 ; x < mediaArrayList.size() ; x++){
                                        System.out.println(mediaArrayList + "mediaArrayList");
                                        System.out.println(databaseImageArrayList + "mediaArrayListDatabase");
                                        if(mediaArrayList.get(x).type == 0){
                                            deleteFiles(mediaArrayList.get(x).mediaUri);
                                            System.out.println(mediaArrayList.get(x).mediaUri.toString() + "localFile1");
                                            mediaArrayList.remove(x);
                                        }
                                    }
                                }else{
                                    for(int i = 0 ; i < mediaArrayList.size() ; i++){
                                        if(!databaseImageArrayList.contains(mediaArrayList.get(i).id) && mediaArrayList.get(i).type == 0){
                                            deleteFiles(mediaArrayList.get(i).mediaUri);
                                            System.out.println(mediaArrayList.get(i).mediaUri.toString() + "localFile");
                                            mediaArrayList.remove(i);
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
    }

    public void getVideosFromStorage()
    {
        databaseVideoArrayList.clear();
        videoIds.clear();
        firebaseFirestore.collection("videos")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String name = document.getId();
                                databaseVideoArrayList.add(name);
                                if (!oldVideoIds.contains(name)) videoIds.add(name);
                            }
                            for (String x : videoIds) {
                                oldVideoIds.add(x);
                                String path = x + ".mp4";
                                String link = "videos/" + x;
                                StorageReference newRef = storageReference.child(link);
                                File localFile = new File(getFilesDir(),path);      //Creating file directory in internal storage
                                videoUri = Uri.fromFile(localFile);
                                Media newVideo = new Media(x, 1, videoUri);
                                if(!localFile.exists()){
                                    System.out.println("VideoYok");
                                    DocumentReference documentReference = firebaseFirestore.collection("videos").document(newVideo.id);
                                    documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            if(documentSnapshot.get("phoneNumber") != null){
                                                String phoneNumber = documentSnapshot.get("phoneNumber").toString();
                                                newVideo.setPhoneNumber(phoneNumber);
                                                mediaArrayList.add(newVideo);
                                            }else{
                                                String phone = "No phone number";
                                                newVideo.setPhoneNumber(phone);
                                                mediaArrayList.add(newVideo);
                                            }

                                        }
                                    });
                                    newRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            System.out.println("Video is downloaded.");
                                            videoCounter++;
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            System.out.println("Video cannot be downloaded.");
                                        }
                                    });
                                }else{
                                    DocumentReference documentReference = firebaseFirestore.collection("videos").document(newVideo.id);
                                    documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            if(documentSnapshot.get("phoneNumber") != null){
                                                String phoneNumber = documentSnapshot.get("phoneNumber").toString();
                                                newVideo.setPhoneNumber(phoneNumber);
                                                mediaArrayList.add(newVideo);
                                                System.out.println("VideoVar");
                                                videoCounter++;
                                            }else{
                                                String phone = "No phoneNumber";
                                                newVideo.setPhoneNumber(phone);
                                                mediaArrayList.add(newVideo);
                                                videoCounter++;
                                            }

                                        }
                                    });
                                }
                            }
                            if(!mediaArrayList.isEmpty()){
                                if(databaseVideoArrayList.isEmpty()){
                                    for(int x = 0 ; x < mediaArrayList.size() ; x++){
                                        System.out.println(mediaArrayList + "mediaArrayVideo");
                                        System.out.println(databaseVideoArrayList + "mediaArrayVideoDatabase");
                                        if(mediaArrayList.get(x).type == 1){
                                            deleteFiles(mediaArrayList.get(x).mediaUri);
                                            mediaArrayList.remove(x);
                                        }
                                    }
                                }else{
                                    for(int i = 0 ; i < mediaArrayList.size() ; i++){
                                        System.out.println(mediaArrayList + "mediaArrayVideo");
                                        System.out.println(databaseVideoArrayList + "mediaArrayVideoDatabase");
                                        if(!databaseVideoArrayList.contains(mediaArrayList.get(i).id) && mediaArrayList.get(i).type == 1){
                                            deleteFiles(mediaArrayList.get(i).mediaUri);
                                            mediaArrayList.remove(i);
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.d("ana","hata");
                        }
                    }
                });
    }

    public void deleteFiles(Uri path){
        File delete = new File(path.getPath());
        if (delete.exists()) {
            if (delete.delete()) {
                System.out.println("file Deleted :" +path.getPath());
            } else {
                System.out.println("file not Deleted :" + path.getPath());
            }
        }
    }
}
