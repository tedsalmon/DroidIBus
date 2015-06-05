package net.littlebigisland.droidibus.activity;
/**
 * Base Dashboard Fragment - Controls base functions
 * and drops in the child fragments 
 * @author Ted <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus.activity
 */

import net.littlebigisland.droidibus.R;
import net.littlebigisland.droidibus.ibus.IBusCallbackReceiver;
import net.littlebigisland.droidibus.ibus.IBusCommandsEnum;
import net.littlebigisland.droidibus.ibus.IBusMessageService;
import net.littlebigisland.droidibus.ibus.IBusMessageService.IOIOBinder;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public class DashboardFragment extends BaseFragment{
    
    protected Handler mHandler = new Handler();
    
    protected SharedPreferences mSettings = null;
    
    protected PowerManager mPowerManager = null;
    protected WakeLock mScreenWakeLock;
    protected boolean mScreenOn = false;
    
    protected boolean mPopulatedFragments = false;
    
    private IBusCallbackReceiver mIBusUpdateListener = new IBusCallbackReceiver() {
        /** Callback to handle Ignition State Updates
         * @param int State of Ignition (0, 1, 2)
         */
        @Override
        public void onUpdateIgnitionSate(final int state) {
            boolean carState = (state > 0) ? true : false;
            changeScreenState(carState);
        }
    };
	
    // Service connection class for IBus
    private ServiceConnection mIBusConnection = new ServiceConnection() {
		
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            IOIOBinder binder = (IOIOBinder) service;
            mIBusService = binder.getService();
            if(mIBusService != null) {
                mIBusConnected = true;
                try {
                    mIBusService.addCallback(mIBusUpdateListener, mHandler);
                } catch (Exception e) {
                    showToast("Unable to start; Cannot bind ourselves to the IBus Service");
                }
                // Emulate BoardMonitor Bootup on connect
                Log.d(TAG, "mIBusService connected and BoardMonitor Bootup Performed");
                sendIBusCommand(IBusCommandsEnum.BMToIKEGetIgnitionStatus);
                sendIBusCommand(IBusCommandsEnum.BMToLCMGetDimmerStatus);
                sendIBusCommand(IBusCommandsEnum.BMToGMGetDoorStatus);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        	Log.d(TAG, "mIBusService disconnected");
            mIBusConnected = false;
        }
    };
    
    /**
     * Acquire a screen wake lock to either turn the screen on or off
     * @param screenState if true, turn the screen on, else turn it off
     */
    @SuppressWarnings("deprecation")
    private void changeScreenState(boolean screenState){
        if(mPowerManager == null){
        	mPowerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        }
        boolean modeChange = false;
        Window window = getActivity().getWindow();
        WindowManager.LayoutParams layoutP = window.getAttributes();
        
        if(screenState && !mScreenOn){
            modeChange = true;
            mScreenOn = true;
            layoutP.screenBrightness = -1;
            mScreenWakeLock = mPowerManager.newWakeLock(
            		PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            		"screenWakeLock"
            );
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD); 
            mScreenWakeLock.acquire();
        }
        
        if(!screenState && mScreenOn){
            Log.d(TAG, "Shutting off the screen");
            modeChange = true;
            mScreenOn = false;
            layoutP.screenBrightness = 0;
            releaseWakelock();
        }
        
        if(modeChange){
            // Set the given layout
            window.setAttributes(layoutP);
        }
    }
	
    private void releaseWakelock(){
        if(mScreenWakeLock != null){
            if(mScreenWakeLock.isHeld()){
                mScreenWakeLock.release();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Dashboard: onCreate Called");
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.dashboard, container, false);
        Log.d(TAG, "Dashboard: onCreateView Called");
        if(!mPopulatedFragments){
            FragmentTransaction tx = getChildFragmentManager().beginTransaction();
	    tx.add(R.id.music_fragment, new DashboardMusicFragment());
	    tx.add(R.id.stats_fragment, new DashboardStatsFragment());
	    tx.commit();
	    mPopulatedFragments = true;
        }
        // Keep a wake lock
        changeScreenState(true);
        return v;
    }
    
    @Override
    public void onActivityCreated (Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "Dashboard: onActivityCreated Called");
        // Bind required background services last since the callback
        // functions depend on the view items being initialized
        if(!mIBusConnected){
            serviceStarter(IBusMessageService.class, mIBusConnection);
        }
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d(TAG, "Dashboard: onDestroy called");
    	releaseWakelock();
    	if(mIBusConnected){
    	    mIBusService.removeCallback(mIBusUpdateListener);
            mIBusService.disable();
            serviceStopper(IBusMessageService.class, mIBusConnection);
    	}
    }
}