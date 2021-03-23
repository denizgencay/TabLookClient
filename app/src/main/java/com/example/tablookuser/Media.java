package com.example.tablookuser;

import android.net.Uri;

public class Media
{
    public String id;
    public int type; //if type == 0 it is an image, if type == 1 it is an video
    public Uri mediaUri;

    public Media (String id, int type, Uri mediaUri)
    {
        this.id = id;
        this.type = type;
        this.mediaUri = mediaUri;
    }
}
