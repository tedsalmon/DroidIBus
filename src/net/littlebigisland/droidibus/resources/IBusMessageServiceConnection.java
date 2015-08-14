package net.littlebigisland.droidibus.resources;

import net.littlebigisland.droidibus.ibus.IBusCommand;
import net.littlebigisland.droidibus.services.IBusMessageService;
import net.littlebigisland.droidibus.services.IBusMessageService.IOIOBinder;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class IBusMessageServiceConnection implements ServiceConnection{
    
    private static final String TAG = "DroidIBus";

    private boolean mServiceConnected = false;
    private IBusMessageService mService = null;
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service){
        mService = ((IOIOBinder) service).getService();
        mServiceConnected = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name){
        mServiceConnected = false;
    }
    
    private void logError(String msg){
        Log.e(TAG, msg);
    }
    
    public boolean isConnected(){
        return mServiceConnected;
    }
    
    /**
     * Get the IBus link state - Useful to check if we can write to the bus
     * @return boolean IBus link state
     */
    public boolean getLinkState(){
        if(mServiceConnected){
            if(mService != null){
                return mService.getLinkState();
            }else{
                Log.e(TAG, "mIBusService is null!");
            }
        }
        return false;
    }
    
    public IBusMessageService getService(){
        return mService;
    }
    
    public void sendCommand(IBusCommand.Commands cmd, final Object... args){
        if(isConnected()){
            mService.sendCommand(new IBusCommand(cmd, args));
        }else{
            logError(String.format("IBusService unbound: Discarding %s", cmd));
        }
    }
    
    public void sendCommandDelayed(IBusCommand.Commands cmd, long delay, Object... args){
        if(isConnected()){
            mService.sendCommandDelayed(new IBusCommand(cmd, args), delay);
        }else{
            logError(String.format("IBusService unbound: Discarding %s", cmd));
        }
    }

}
