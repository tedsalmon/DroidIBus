package net.littlebigisland.droidibus.resources;

import net.littlebigisland.droidibus.services.IBusMessageService;
import net.littlebigisland.droidibus.services.IBusMessageService.IOIOBinder;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class IBusMessageServiceConnection implements ServiceConnection{

    private boolean mServiceConnected = false;
    private IBusMessageService mIBusService = null;
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service){
        mIBusService = ((IOIOBinder) service).getService();
        mServiceConnected = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name){
        mServiceConnected = false;
    }
    
    public boolean isConnected(){
        return mServiceConnected;
    }
    
    public IBusMessageService getService(){
        return mIBusService;
    }

}
