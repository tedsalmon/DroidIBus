package net.littlebigisland.droidibus.activity;
/**
 * Settings Fragment
 * @author Ted <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus.activity
 */
import java.util.Calendar;

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
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener{
    
	public String TAG = "DroidIBus";
    private Handler mHandler = new Handler();
    protected IBusMessageService mIBusService;
    protected boolean mIBusConnected = false;
    
    private Preference mOBCTime = null;
    private Preference mOBCDate = null;
    
    private SharedPreferences mSettings = null;
	
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
		if(mIBusConnected && mIBusService.getLinkState()){
		    mIBusService.sendCommand(new IBusCommand(cmd, args));
		}
    }
	
    public void showToast(String toastText){
        Context appContext = getActivity();
        Toast.makeText(appContext, toastText, Toast.LENGTH_LONG).show();
    }
    
    
    private IBusCallbackReceiver mIBusUpdateListener = new IBusCallbackReceiver(){
        
	    /**
	     * Update the time picker object with the time from the IKE
	     */
	    @Override
	    public void onUpdateTime(final String time){
	        String[] mTimeParts = {};
	        if(time.contains("AM") || time.contains("PM")){
	            // Convert AM/PM to 24-hour clock for use in settings
	            // Make the spaces zeros, get the time without AM/PM then split by the separator
	            mTimeParts = time.replace(" ", "0").substring(0, 5).split(":");
	            // Covert to 24 hour clock by adding 12 hours if it's PM
	            if(time.contains("PM")){
	                mTimeParts[0] = String.valueOf(Integer.parseInt(mTimeParts[0]) + 12);
	            }
	        }else{
	            // 24 hour clock so nothing to do but split
	            mTimeParts = time.split(":");
	        }
	        mOBCTime.setDefaultValue(
	            String.format("%s:%s", mTimeParts[0], mTimeParts[1])
	        );
	    }
	        
	    /**
	     * Update the date picker object with the date from the IKE
	     */
	    @Override
	    public void onUpdateDate(final String date){
	        // Check to see what the date format is
	        if(date.contains(".")){
	            String[] tDate = date.split(".");
	            mOBCDate.setDefaultValue(
	                String.format("%s-%s-%s", tDate[2], tDate[1], tDate[0])
	            );
	        }else{
	            String[] tDate = date.split("/");
	            mOBCDate.setDefaultValue(
	                String.format("%s-%s-%s", tDate[2], tDate[0], tDate[1])
	            );
	        }
	    }
    
	    @Override
	    public void onUpdateUnits(String units){
	        // Split the binary strings spat out by space
	        String[] unitTypes = units.split(";");
	        // Split the binary into an array
	        String[] aUnits = unitTypes[0].split("(?!^)");
	        String[] cUnits = unitTypes[1].split("(?!^)");
	        // Access each array for the values of the units
	        mSettings.edit().putString("speedUnit", aUnits[3]).apply();
	        mSettings.edit().putString("distanceUnit", aUnits[1]).apply();
	        mSettings.edit().putString("temperatureUnit", aUnits[6]).apply();
	        mSettings.edit().putString("timeUnit", aUnits[7]).apply();
	        mSettings.edit().putString("consumptionUnit", cUnits[6]+cUnits[7]).apply();
	    }

	};
    
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
                    e.printStackTrace();
                }
                sendIBusCommand(IBusCommandsEnum.BMToIKEGetTime);
                sendIBusCommand(IBusCommandsEnum.BMToIKEGetDate);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "mIBusService is disconnected");
            mIBusConnected = false;
        }
    };

    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        addPreferencesFromResource(R.xml.settings_data);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        final View v = inflater.inflate(R.layout.settings, container, false);
        mSettings = getPreferenceManager().getSharedPreferences();
        return v;
    }
    
    @Override
    public void onActivityCreated (Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG, "Settings: onActivityCreated Called");
        mOBCTime = (Preference) findPreference("obcTime");
        mOBCDate = (Preference) findPreference("obcDate");
        Preference syncCarDateTime = (Preference) findPreference("syncDateTime");
        
        syncCarDateTime.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Calendar tDate = Calendar.getInstance();
                sendIBusCommand(
                    IBusCommandsEnum.BMToIKESetDate,
                    tDate.get(Calendar.DATE), 
                    tDate.get(Calendar.MONTH) + 1,
                    tDate.get(Calendar.YEAR) - 2000
                );
                sendIBusCommand(
                    IBusCommandsEnum.BMToIKESetTime,
                    tDate.get(Calendar.HOUR_OF_DAY), 
                    tDate.get(Calendar.MINUTE)
                );
                showToast("Successfully synced date and time with car!");
                return true;
            }
        });
        // Bind required background services last since the callback
        // functions depend on the view items being initialized
        if(!mIBusConnected){
        	serviceStarter(IBusMessageService.class, mIBusConnection);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key){
        if(!mIBusConnected){
        	serviceStarter(IBusMessageService.class, mIBusConnection);
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
                String[] time = sharedPreferences.getString(key, "").split(":");
                int hour = Integer.parseInt(time[0]);
                int minute = Integer.parseInt(time[1]);
                sendIBusCommand(IBusCommandsEnum.BMToIKESetTime, hour, minute);
                prefVal = sharedPreferences.getString(key, "");
                break;
            case "settingRadioType":
                prefVal = sharedPreferences.getString(key, "");
                break;
            case "nightColorsWithInterior":
            case "navAvailable":
            case "stealthOneAvailable":
                prefVal = (sharedPreferences.getBoolean(key, false)) ? "true" : "false";
                break;
            case "timeUnit":
            case "distanceUnit":
            case "speedUnit":
            case "temperatureUnit":
            case "consumptionUnit":
                prefVal = sharedPreferences.getString(key, "");
                sendIBusCommand(
                    IBusCommandsEnum.BMToIKESetUnits,
                    Integer.parseInt(sharedPreferences.getString("speedUnit", "1")),
                    Integer.parseInt(sharedPreferences.getString("distanceUnit", "1")),
                    Integer.parseInt(sharedPreferences.getString("temperatureUnit", "1")),
                    Integer.parseInt(sharedPreferences.getString("timeUnit", "0")),
                    Integer.parseInt(sharedPreferences.getString("consumptionUnit", "01"))
                );
                break;
        }
        Log.d(TAG, String.format("Got preference change for %s -> %s", key, prefVal));
    }
    
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        Log.d(TAG, "Settings: onResume Called");
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
        Log.d(TAG, "Settings: onPause Called");
    }
    
    @Override
    public void onDestroy(){
        super.onDestroy();
        mIBusService.removeCallback(mIBusUpdateListener);
        Log.d(TAG, "Settings: onDestroy Called");
        if(mIBusConnected){
        	serviceStopper(IBusMessageService.class, mIBusConnection);
        }
    }

}