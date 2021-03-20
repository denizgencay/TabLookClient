package com.example.tablookuser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {

    private FirebaseStorage firebaseStorage;
    private FirebaseFirestore firebaseFirestore;
    private StorageReference storageReference;
    private ImageView imageView;
    private VideoView videoView;
    private Uri imageUri, videoUri;
    private ArrayList<String> imageIds, videoIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);
        getDataFromFirebase();
        imageView = findViewById(R.id.imageView);
        videoView = findViewById(R.id.videoView);
        imageIds = new ArrayList<>();
        videoIds = new ArrayList<>();
    }


    private void getDataFromFirebase() {

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
                                String link = "images/" + x;
                                StorageReference newRef = storageReference.child(link);

                                File localFile = null;
                                try {
                                    localFile = File.createTempFile(x, ".jpg");
                                    imageUri = Uri.fromFile(localFile);
                                    System.out.println(localFile.toString() + "buaraya");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                newRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                        System.out.println("great");
                                        imageView.setImageURI(imageUri);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        System.out.println("not great");
                                    }
                                });
                            }

                        } else {
                            Log.d("ana","hata");
                        }
                    }
                });
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
//                                        System.out.println("great");
//                                        videoView.setVideoURI(videoUri);
                                        videoView.setVideoURI(videoUri);
                                        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                            @Override
                                            public void onPrepared(MediaPlayer mp) {
                                                mp.setLooping(true);
                                                videoView.start();
                                            }
                                        });

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        System.out.println("not great");
                                    }
                                });
                            }

                        } else {
                            Log.d("ana","hata");
                        }
                    }
                });

    }
}