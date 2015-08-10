package net.littlebigisland.droidibus.resources;

import net.littlebigisland.droidibus.services.MusicControllerService;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class MusicControllerServiceConnection implements ServiceConnection{

    private boolean mServiceConnected = false;
    private MusicControllerService mService = null;
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service){
        MusicControllerService.MusicControllerBinder serviceBinder = (
            MusicControllerService.MusicControllerBinder
        ) service;
        mService = serviceBinder.getService();
        mServiceConnected = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name){
        mServiceConnected = false;
    }
    
    public MusicControllerService getService(){
        return mService;
    }
    
    public boolean isConnected(){
        return mServiceConnected;
    }

}