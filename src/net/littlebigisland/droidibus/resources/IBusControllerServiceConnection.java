package net.littlebigisland.droidibus.resources;

import net.littlebigisland.droidibus.services.IBusControllerService;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class IBusControllerServiceConnection implements ServiceConnection{
    
    private boolean mServiceConnected = false;
    private IBusControllerService mService = null;
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service){
        IBusControllerService.IBusControllerBinder serviceBinder = (
            IBusControllerService.IBusControllerBinder
        ) service;
        mService = serviceBinder.getService();
        mServiceConnected = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name){
        mServiceConnected = false;
    }
    
    public IBusControllerService getService(){
        return mService;
    }
    
    public boolean isConnected(){
        return mServiceConnected;
    }

}
