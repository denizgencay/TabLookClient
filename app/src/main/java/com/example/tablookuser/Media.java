package com.example.tablookuser;

import android.net.Uri;

public class Media
{
    public long videoDuration;
    public String id, phoneNumber;
    public int type; //if type == 0 it is an image, if type == 1 it is an video
    public Uri mediaUri;

    public Media (String id, int type,Uri uri )
    {
        this.id = id;
        this.type = type;
        mediaUri = uri;
    }
    public void setPhoneNumber( String number){
        this.phoneNumber = number;
    }
    public void setVideoDuration(long duration){
        videoDuration = duration;
    }
}
