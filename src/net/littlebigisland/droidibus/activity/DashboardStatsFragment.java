package net.littlebigisland.droidibus.activity;

import net.littlebigisland.droidibus.R;
import net.littlebigisland.droidibus.ibus.IBusCommandsEnum;
import net.littlebigisland.droidibus.ibus.IBusCallbackReceiver;
import net.littlebigisland.droidibus.ibus.IBusMessageService;
import net.littlebigisland.droidibus.ibus.IBusMessageService.IOIOBinder;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class DashboardStatsFragment extends BaseFragment{
	public String TAG = "DroidIBusStatsFragment";

	// Fields in the activity
	protected TextView 
		speedUnit, avgSpeedUnit, rangeFieldUnit, consumption1Unit,
		consumption2Unit, outdoorTempUnit, coolantTempUnit,
		speedField, rpmField, rangeField, outTempField,
		coolantTempField, fuel1Field, fuel2Field, avgSpeedField,
		geoCoordinatesField ,geoStreetField, geoLocaleField,
		geoAltitudeField, dateField, timeField;
	
	private IBusCallbackReceiver mIBusUpdateListener = new IBusCallbackReceiver(){
		
	};
	
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
					e.printStackTrace();
				}
				sendIBusCommand(IBusCommandsEnum.BMToIKEGetTime);
				sendIBusCommand(IBusCommandsEnum.BMToIKEGetDate);
    		}
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        	Log.e(TAG, "mIBusService is disconnected");
            mIBusBound = false;
        }
    };
	
	private void bindServices() {
		serviceStarter(IBusMessageService.class, mIBusConnection);
	}
	
	private void unbindServices() {
		if(mIBusBound){
			mIBusService.disable();
			serviceStopper(IBusMessageService.class, mIBusConnection);
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setRetainInstance(true);
		final View v = inflater.inflate(R.layout.settings, container, false);
		return v;
	}
	
	@Override
	public void onActivityCreated (Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
		Log.d(TAG, "Settings: onActivityCreated Called");
		// Bind required background services last since the callback
		// functions depend on the view items being initialized
		if(!mIBusBound){
			bindServices();
		}
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    Log.d(TAG, "Settings: onResume Called");
	}

	@Override
	public void onPause() {
	    super.onPause();
	    Log.d(TAG, "Settings: onPause Called");
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		mIBusService.removeCallback(mIBusUpdateListener);
		Log.d(TAG, "Settings: onDestroy Called");
		if(mIBusBound){
			unbindServices();
		}
	}
}