package net.littlebigisland.droidibus.ui;

/**
 * Base Fragment - Implements universal functionality
 * @author Ted <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus.activity
 */
import java.util.Calendar;

import net.littlebigisland.droidibus.resources.ServiceManager;
import net.littlebigisland.droidibus.resources.ThreadExecutor;
import net.littlebigisland.droidibus.services.IBusMessageService;
import net.littlebigisland.droidibus.services.IBusMessageService.IOIOBinder;

import net.littlebigisland.droidibus.ibus.IBusCommand;
import net.littlebigisland.droidibus.ibus.IBusSystem;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class BaseFragment extends Fragment{
    
    public String TAG = "DroidIBus";
    public String CTAG = "";
    
    public Handler mHandler = new Handler();
    public Context mContext = null;

    // NEVER override these in a child class! The service won't connect
    public IBusMessageService mIBusService = null;
    public boolean mIBusConnected = false;
    
    protected ThreadExecutor mThreadExecutor = new ThreadExecutor();
    
    public class IBusServiceConnection implements ServiceConnection{
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            mIBusService = ((IOIOBinder) service).getService();
            Log.d(TAG, CTAG + "IBus service connected");
            mIBusConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name){
            Log.d(TAG, CTAG + "IBus service disconnected");
            mIBusConnected = false;
        }

    };
    
    /**
     * Change TextView colors recursively; Used to support night colors
     */
    public void changeTextColors(ViewGroup view, int colorId){
        for(int i = 0; i < view.getChildCount(); i++){
            View child = view.getChildAt(i);
            // TextView, change it's color
            if(child instanceof TextView){
                TextView c = (TextView) child;
                c.setTextColor(getResources().getColor(colorId));
            } // ViewGroup; Recurse children to find TextViews
            else if(child instanceof ViewGroup){
                changeTextColors((ViewGroup) child, colorId);
            }
        }
    }
    
    /**
     * Get the IBus link state - Useful to check if we can write to the bus
     * @return boolean IBus link state
     */
    public boolean getIBusLinkState(){
        if(mIBusConnected){
            if(mIBusService != null){
                return mIBusService.getLinkState();
            }else{
                // This shouldn't happen!
                Log.e(TAG, CTAG + "mIBusService is null!");
                showToast(
                    "DroidIBus has encountered an IBus connection error and must stop"
                );
                getActivity().finish();
            }
        }
        return false;
    }
    
    /**
     * Wrapper that returns the time in milliseconds
     * @return Time in Miliseconds from Calendar Module
     */
    public long getTimeNow(){
        return Calendar.getInstance().getTimeInMillis();
    }
    
    /**
     * Register a callback with the IBus Service
     */
    public void registerIBusCallback(IBusSystem.Callbacks cb, Handler handle){
        try{
            mIBusService.registerCallback(cb, handle);
        }catch (Exception e) {
            showToast(
                "ERROR: Could not register callback with the IBus Service"
            );
        }
    }

    @SuppressWarnings("rawtypes")
    public void serviceStarter(Class cls, ServiceConnection svcConn){
        boolean res = ServiceManager.startService(cls, svcConn, mContext);
        if(res){
            Log.d(TAG, CTAG + "Starting " + cls.toString());
        }else{
            Log.e(TAG, CTAG + "Unable to start " + cls.toString());
        }
    }
    
    @SuppressWarnings("rawtypes")
    public void serviceStopper(Class cls, ServiceConnection svcConn){
        boolean res = ServiceManager.stopService(cls, svcConn, mContext);
        if(res){
            Log.d(TAG, CTAG + "Unbinding from " + cls.toString());
        }else{
            Log.e(TAG, CTAG + "Unable to unbind from " + cls.toString());
        }
    }

    public void sendIBusCommand(IBusCommand.Commands cmd, final Object... args){
        if(mIBusConnected){
            mIBusService.sendCommand(new IBusCommand(cmd, args));
        }else{
            Log.e(
               TAG, 
               String.format("Discarding command %s because IBusService is unbound", cmd)
            );
        }
    }
    
    public void sendIBusCommandDelayed(final IBusCommand.Commands cmd, 
            final long delayMils, final Object... args){
        mHandler.postDelayed(new Runnable(){
            public void run(){
                sendIBusCommand(cmd, args);
            }
        }, delayMils);
    }
    
    public void showToast(String toastText){
        Context appContext = getActivity();
        Toast.makeText(appContext, toastText, Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
    }
    
    @Override
    public void onDestroy(){
        super.onDestroy();
        mThreadExecutor.terminateTasks();
    }
}