package net.littlebigisland.droidibus.ui;

/**
 * Base Fragment - Implements universal functionality
 * @author Ted <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus.activity
 */

import net.littlebigisland.droidibus.resources.IBusMessageServiceConnection;
import net.littlebigisland.droidibus.resources.ServiceManager;
import net.littlebigisland.droidibus.resources.ThreadExecutor;
import net.littlebigisland.droidibus.services.IBusMessageService;

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
    public String CTAG = getClass().getSimpleName() + ": ";
    
    public Handler mHandler = new Handler();
    public Context mContext = null;

    // NEVER override these in a child class! The service won't connect
    public IBusMessageService mIBusService = null;
    public IBusSystem.Callbacks mIBusCallbacks = null;
    
    protected ThreadExecutor mThreadExecutor = new ThreadExecutor();
    
    public IBusMessageServiceConnection mIBusConnection = new IBusMessageServiceConnection(){
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            super.onServiceConnected(name, service);
            mIBusService = getService();
            Log.d(TAG, "IBus onServiceConnected");
            if(mIBusCallbacks != null){
                Log.d(TAG, "IBusCallback is not null");
                mIBusService.registerCallback(mIBusCallbacks, mHandler);
            }else{
                Log.e(TAG, CTAG + "IBusCallback is null");
            }
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

    @SuppressWarnings("rawtypes")
    public void serviceStarter(Class cls, ServiceConnection svcConn){
        boolean res = ServiceManager.startService(cls, svcConn, mContext);
        if(res){
            Log.d(TAG, CTAG + "Started " + cls.toString());
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
    
    public void showToast(String toastText){
        Toast.makeText(mContext, toastText, Toast.LENGTH_LONG).show();
    }
    
    public void startIBusMessageService(){
        if(mIBusConnection != null){
            serviceStarter(IBusMessageService.class, mIBusConnection);
        }
    }
    
    public void stopIBusMessageService(){
        if(mIBusConnection != null && mIBusService != null){
            if(mIBusCallbacks != null){
                mIBusService.unregisterCallback(mIBusCallbacks);
            }
            serviceStopper(IBusMessageService.class, mIBusConnection);
        }
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