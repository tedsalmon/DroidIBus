package net.littlebigisland.droidibus.activity;


import net.littlebigisland.droidibus.R;
import net.littlebigisland.droidibus.ibus.IBusCommand;
import net.littlebigisland.droidibus.ibus.IBusCommandsEnum;
import net.littlebigisland.droidibus.ibus.IBusCallbackReceiver;
import net.littlebigisland.droidibus.ibus.IBusMessageService;
import net.littlebigisland.droidibus.ibus.IBusMessageService.IOIOBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener{
	public String TAG = "DroidIBus";
	
	private Handler mHandler = new Handler();
	protected IBusMessageService mIBusService;
	protected boolean mIBusBound = false;
	
	private IBusCallbackReceiver mIBusUpdateListener = new IBusCallbackReceiver(){
		
		@Override
		public void onUpdateTime(final String time){
			//timeField.setText(time);
		}
		
		@Override
		public void onUpdateDate(final String date){
			//dateField.setText(date);
		}
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
		Context applicationContext = getActivity();
		
		Intent IBusIntent = new Intent(applicationContext, IBusMessageService.class);
		try {
			Log.d(TAG, "Starting IBus service");
			applicationContext.bindService(IBusIntent, mIBusConnection, Context.BIND_AUTO_CREATE);
			applicationContext.startService(IBusIntent);
		}
		catch(Exception ex) {
			Log.e(TAG, "Unable to Start IBusService!");
		}
	}
	
	private void unbindServices() {
		Context applicationContext = getActivity();
		if(mIBusBound){
			try {
				Log.d(TAG, "Unbinding from IBusMessageService");
				mIBusService.disable();
				applicationContext.unbindService(mIBusConnection);
				applicationContext.stopService(
					new Intent(applicationContext, IBusMessageService.class)
				);
				mIBusBound = false;
			}
			catch(Exception ex) {
				Log.e(TAG, String.format("Unable to unbind the IBusMessageService - '%s'!", ex.getMessage()));
			}
		}
	}
	
	private void sendIBusCommand(final IBusCommandsEnum cmd, final Object... args){
		if(mIBusBound && mIBusService.isIBusActive()){
			mIBusService.sendCommand(new IBusCommand(cmd, args));
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.settings, container, false);
		addPreferencesFromResource(R.xml.settings_data);
		if(!mIBusBound){
			bindServices();
		}
		return v;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key){
		if(!mIBusBound){
			bindServices();
		}
		String prefVal = "";
		switch(key){
			case "obcDate":
				String dateParts[] = sharedPreferences.getString(key, "").split(" ");
				int day = Integer.parseInt(dateParts[0]);
				int month = Integer.parseInt(dateParts[1]);
				int year = Integer.parseInt(dateParts[2]);
				prefVal = String.format("%s %s %s", day, month, year);
				sendIBusCommand(IBusCommandsEnum.BMToIKESetDate, day, month, year);
				break;
			case "obcTime":
				String time = sharedPreferences.getString(key, "");
				int hour = Integer.parseInt(time.substring(0, 2));
				int minute = Integer.parseInt(time.substring(3, 5));
				sendIBusCommand(IBusCommandsEnum.BMToIKESetTime, hour, minute);
				prefVal = time;
				break;
			case "settingRadioType":
				prefVal = sharedPreferences.getString(key, "");
				break;
			case "nightColorsWithInterior":
			case "navAvailable":
				prefVal = (sharedPreferences.getBoolean(key, false)) ? "true" : "false";
				break;
		}
		Log.d(TAG, String.format("Got preference change for %s -> %s", key, prefVal));
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

	}

	@Override
	public void onPause() {
	    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	    super.onPause();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		mIBusService.removeCallback(mIBusUpdateListener);
		if(mIBusBound){
			unbindServices();
		}
	}
}