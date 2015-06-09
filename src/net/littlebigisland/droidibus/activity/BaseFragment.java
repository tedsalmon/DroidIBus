package net.littlebigisland.droidibus.activity;

/**
 * Base Fragment - Implements universal functionality
 * @author Ted <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus.activity
 */

//import net.littlebigisland.droidibus.ibus.IBusCallbackReceiver;
import net.littlebigisland.droidibus.ibus.IBusCommand;
import net.littlebigisland.droidibus.ibus.IBusSystem;
import net.littlebigisland.droidibus.ibus.IBusMessageService;
import net.littlebigisland.droidibus.ibus.IBusMessageService.IOIOBinder;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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

    // NEVER override these in a child class! The service won't connected
    public IBusMessageService mIBusService = null;
    public boolean mIBusConnected = false;
    
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
        Context applicationContext = getActivity();
        Intent svcIntent = new Intent(applicationContext, cls);
        try{
            Log.d(
                TAG,
                String.format("%sStarting %s service", CTAG, cls.toString())
            );
            applicationContext.bindService(
                svcIntent, svcConn, Context.BIND_AUTO_CREATE
            );
            applicationContext.startService(svcIntent);
        }
        catch(Exception ex){
            Log.d(
                TAG,
                String.format(
                    "%sUnable to start %s service", CTAG, cls.toString()
                )
            );
        }
    }
    
    @SuppressWarnings("rawtypes")
    public void serviceStopper(Class cls, ServiceConnection svcConn){
        Context applicationContext = getActivity();
        try{
            Log.d(
                TAG,
                String.format(
                    "%sUnbinding from %s service", CTAG, cls.toString()
                )
            );
            applicationContext.unbindService(svcConn);
        }
        catch(Exception ex){
            Log.e(
                TAG,
                String.format(
                    "%sUnable to unbind %s - Exception '%s'!",
                    CTAG,
                    cls.toString(),
                    ex.getMessage()
                )
            );
        }
    }

    public void sendIBusCommand(
        final IBusCommand.Commands cmd, final Object... args
    ){
        if(mIBusConnected && mIBusService.getLinkState()){
            mIBusService.sendCommand(new IBusCommand(cmd, args));
        }
    }
    
    public void sendIBusCommandDelayed(
        final IBusCommand.Commands cmd, 
        final long delayMils, final Object... args
    ){
        new Handler(getActivity().getMainLooper()).postDelayed(new Runnable(){
            public void run(){
                sendIBusCommand(cmd, args);
            }
        }, delayMils);
    }
    
    public void showToast(String toastText){
        Context appContext = getActivity();
        Toast.makeText(appContext, toastText, Toast.LENGTH_LONG).show();
    }
}