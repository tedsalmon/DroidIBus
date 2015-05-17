package net.littlebigisland.droidibus.activity;

import net.littlebigisland.droidibus.ibus.IBusCommand;
import net.littlebigisland.droidibus.ibus.IBusCommandsEnum;
import net.littlebigisland.droidibus.ibus.IBusMessageService;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class BaseFragment extends Fragment{
    public String TAG = "DroidIBus";
    
    public Handler mHandler = new Handler();
    public IBusMessageService mIBusService;
    public boolean mIBusBound = false;
	
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
        Context applicationContext = getActivity();
        Intent svcIntent = new Intent(applicationContext, cls);
        try {
            Log.d(TAG, String.format("Starting %s Service", cls.toString()));
            applicationContext.bindService(svcIntent, svcConn, Context.BIND_AUTO_CREATE);
            applicationContext.startService(svcIntent);
        }
        catch(Exception ex) {
            Log.d(TAG, String.format("Unable to start %s Service", cls.toString()));
        }
    }
	
    @SuppressWarnings("rawtypes")
    public void serviceStopper(Class cls, ServiceConnection svcConn){
        Context applicationContext = getActivity();
        Intent svcIntent = new Intent(applicationContext, cls);
        try {
            Log.d(TAG, String.format("Unbinding from  %s Service", cls.toString()));
            applicationContext.unbindService(svcConn);
            applicationContext.stopService(svcIntent);
        }
        catch(Exception ex) {
            Log.e(TAG, String.format("Unable to unbind the %s - '%s'!", cls.toString(), ex.getMessage()));
        }
    }

    public void sendIBusCommand(final IBusCommandsEnum cmd, final Object... args){
		if(mIBusBound && mIBusService.getLinkState()){
		    mIBusService.sendCommand(new IBusCommand(cmd, args));
		}
    }
	
    public void sendIBusCommandDelayed(final IBusCommandsEnum cmd, final long delayMillis, final Object... args){
		new Handler(getActivity().getMainLooper()).postDelayed(new Runnable(){
		    public void run(){
			sendIBusCommand(cmd, args);
		    }
		}, delayMillis);
    }
	
    public void showToast(String toastText){
        Context appContext = getActivity();
        Toast.makeText(appContext, toastText, Toast.LENGTH_LONG).show();
    }
}