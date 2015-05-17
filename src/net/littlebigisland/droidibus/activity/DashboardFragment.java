package net.littlebigisland.droidibus.activity;
/**
 * Control Fragment for IBus UI 
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus.activity
 */
import net.littlebigisland.droidibus.R;
import net.littlebigisland.droidibus.ibus.IBusCallbackReceiver;
import net.littlebigisland.droidibus.ibus.IBusMessageService;
import net.littlebigisland.droidibus.ibus.IBusMessageService.IOIOBinder;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.BatteryManager;
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

public class DashboardFragment extends BaseFragment {
    public String TAG = "DroidIBus";
    
    protected Handler mHandler = new Handler();
    
    protected SharedPreferences mSettings = null;
    
    protected PowerManager mPowerManager = null;
    protected WakeLock mScreenWakeLock;
    protected boolean mScreenOn = false;
    
    protected IBusMessageService mIBusService;
    protected boolean mIBusBound = false;
	
    private BroadcastReceiver mChargingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int chargeType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean isCharging = chargeType == BatteryManager.BATTERY_PLUGGED_USB || chargeType == BatteryManager.BATTERY_PLUGGED_AC;
            carPowerChange(isCharging);
        }
    };
    
    private IBusCallbackReceiver mIBusUpdateListener = new IBusCallbackReceiver() {
        /** Callback to handle Ignition State Updates
         * @param int State of Ignition (0, 1, 2)
         */
        @Override
        public void onUpdateIgnitionSate(final int state) {
            boolean carState = (state > 0) ? true : false;
            carPowerChange(carState);
        }
    };
	
    // Service connection class for IBus
    private ServiceConnection mIBusConnection = new ServiceConnection() {
		
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            IOIOBinder binder = (IOIOBinder) service;
            mIBusService = binder.getService();
            if(mIBusService != null) {
                mIBusBound = true;
                try {
                        mIBusService.addCallback(mIBusUpdateListener, mHandler);
                } catch (Exception e) {
                        showToast("Unable to start; Cannot bind ourselves to the IBus Service");
                }
                // Emulate BoardMonitor Bootup on connect
                Log.d(TAG, "mIBusService connected and BoardMonitor Bootup Performed");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        	Log.d(TAG, "mIBusService disconnected");
            mIBusBound = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().registerReceiver(mChargingReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        Log.d(TAG, "Dashboard: onCreate Called");
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.dashboard, container, false);
        Log.d(TAG, "Dashboard: onCreateView Called");
        Fragment musicPlayerFragment = new DashboardMusicFragment();
        Fragment statsFragment = new DashboardStatsFragment();
        getChildFragmentManager().beginTransaction().add(R.id.music_fragment, musicPlayerFragment).commit();
        getChildFragmentManager().beginTransaction().add(R.id.stats_fragment, statsFragment).commit();
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
        if(!mIBusBound){
            serviceStarter(IBusMessageService.class, mIBusConnection);
        }
    }
	

	
    /**
     * Acquire a screen wake lock to either turn the screen on or off
     * @param screenState if true, turn the screen on, else turn it off
     */
    @SuppressWarnings("deprecation")
    private void changeScreenState(boolean screenState){
        if(mPowerManager == null) mPowerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        boolean modeChange = false;
        Window window = getActivity().getWindow();
        WindowManager.LayoutParams layoutP = window.getAttributes();
        
        if(screenState && !mScreenOn){
            modeChange = true;
            mScreenOn = true;
            layoutP.screenBrightness = -1;
            mScreenWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "screenWakeLock");
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
            window.setAttributes(layoutP); // Set the given layout
        }
    }
	
    private void releaseWakelock(){
        if(mScreenWakeLock != null){
            if(mScreenWakeLock.isHeld()){
                mScreenWakeLock.release();
            }
        }
    }
    
	
    private void carPowerChange(boolean isCarOn){
        Log.d(TAG, "Charging state change!");
    	if(isCarOn){
    	    // The screen isn't on but the car is, turn it on
    	    //if(!mScreenOn){}
    	}else{
            // The car is not on and the screen is, turn it off
            if(mScreenOn){
                changeScreenState(false);
            }
    	}
    }
	
    @Override
    public void onPause() {
    	super.onPause();
    	Log.d(TAG, "Dashboard: onPause called");
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.d(TAG, "Dashboard: onResume called");
    	if(mIBusBound){
            Log.d(TAG, "Dashboard: IOIO bound in onResume");
            if(!mIBusService.getLinkState()){
                serviceStopper(IBusMessageService.class, mIBusConnection);
                serviceStarter(IBusMessageService.class, mIBusConnection);
                Log.d(TAG, "Dashboard: IOIO Not connected in onResume");
            }
    	}else{
            Log.d(TAG, "Dashboard: IOIO NOT bound in onResume");
            serviceStarter(IBusMessageService.class, mIBusConnection);
    	}
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d(TAG, "Dashboard: onDestroy called");
    	mIBusService.removeCallback(mIBusUpdateListener);
    	releaseWakelock();
    	if(mIBusBound){
            mIBusService.disable();
            serviceStopper(IBusMessageService.class, mIBusConnection);
    	}
    	getActivity().unregisterReceiver(mChargingReceiver);
    }
}