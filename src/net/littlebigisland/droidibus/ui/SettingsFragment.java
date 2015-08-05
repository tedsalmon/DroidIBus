package net.littlebigisland.droidibus.ui;
/**
 * Settings Fragment
 * @author Ted <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus.activity
 */
import java.util.Calendar;

import net.littlebigisland.droidibus.resources.ServiceManager;
import net.littlebigisland.droidibus.services.IBusMessageService;
import net.littlebigisland.droidibus.services.IBusMessageService.IOIOBinder;

import net.littlebigisland.droidibus.R;
import net.littlebigisland.droidibus.ibus.IBusCommand;
import net.littlebigisland.droidibus.ibus.IBusSystem;
import android.content.ComponentName;
import android.content.Context;
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

public class SettingsFragment extends PreferenceFragment{
    
    public String TAG = "DroidIBus";
    public String CTAG = "SettingsFragment: ";
    private Handler mHandler = new Handler();
    protected IBusMessageService mIBusService;
    protected boolean mIBusConnected = false;
    
    protected Context mContext = null;
    
    private Preference mOBCTime = null;
    private Preference mOBCDate = null;
    
    private SharedPreferences mSettings = null;
    
    private IBusSystem.Callbacks mIBusCallbacks = new IBusSystem.Callbacks(){
        /**
         * Update the time picker object with the time from the IKE
         */
        @Override
        public void onUpdateTime(final String time){
            String[] mTimeParts = {};
            if(time.contains("AM") || time.contains("PM")){
                // Convert AM/PM to 24 hour time to use in settings
                // Make the spaces zeros, get the time without 
		// the AM/PM then split by the separator
                mTimeParts = time.replace(" ", "0").substring(0, 5).split(":");
                // Convert to 24 hour clock by adding 12 hours if it's PM
                if(time.contains("PM")){
                    mTimeParts[0] = String.valueOf(
			Integer.parseInt(mTimeParts[0]) + 12
		    );
                }
            }else{
                // 24 hour time so nothing to do but split
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
            mSettings.edit().putString(
		"consumptionUnit", cUnits[6]+cUnits[7]
	    ).apply();
        }
    };
    
    private ServiceConnection mIBusConnection = new ServiceConnection(){
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder svc){
            mIBusService = ((IOIOBinder) svc).getService();
	    Log.d(TAG, CTAG + "IBus service connected");
            mIBusConnected = true;
	    try{
		mIBusService.registerCallback(mIBusCallbacks, mHandler);
	    }catch (Exception e) {
		showToast(
		    "ERROR: Could not register callback with the IBus Service"
		);
	    }
	    sendIBusCommand(IBusCommand.Commands.GFXToIKEGetTime);
	    sendIBusCommand(IBusCommand.Commands.GFXToIKEGetDate);
        }

        @Override
        public void onServiceDisconnected(ComponentName name){
            Log.d(TAG, CTAG + "IBus service disconnected");
            mIBusConnected = false;
        }

    };
    
    private Preference.OnPreferenceClickListener mPrefClickListener =
	new Preference.OnPreferenceClickListener(){
	    @Override
	    public boolean onPreferenceClick(Preference preference){
		Calendar tDate = Calendar.getInstance();
		sendIBusCommand(
		    IBusCommand.Commands.GFXToIKESetDate,
		    tDate.get(Calendar.DATE), 
		    tDate.get(Calendar.MONTH) + 1,
		    tDate.get(Calendar.YEAR) - 2000
		);
		sendIBusCommand(
		    IBusCommand.Commands.GFXToIKESetTime,
		    tDate.get(Calendar.HOUR_OF_DAY), 
		    tDate.get(Calendar.MINUTE)
		);
		showToast("Successfully synced date and time with car!");
		return true;
	    }
	};
    
    private OnSharedPreferenceChangeListener mSharedPrefListener =
	new OnSharedPreferenceChangeListener(){
    
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sPrefs, String key){
	    String prefVal = null;
	    switch(key){
		case "obcDate":
		    String dateParts[] = sPrefs.getString(key, "").split(" ");
		    int day = Integer.parseInt(dateParts[0]);
		    int month = Integer.parseInt(dateParts[1]);
		    int year = Integer.parseInt(dateParts[2]);
		    prefVal = String.format("%s %s %s", day, month, year);
		    sendIBusCommand(
		        IBusCommand.Commands.GFXToIKESetDate, day, month, year
		    );
		    break;
		case "obcTime":
		    String[] time = sPrefs.getString(
			key, ""
		    ).split(":");
		    int hour = Integer.parseInt(time[0]);
		    int minute = Integer.parseInt(time[1]);
		    sendIBusCommand(
		        IBusCommand.Commands.GFXToIKESetTime, hour, minute
		    );
		    prefVal = sPrefs.getString(key, "");
		    break;
		case "settings_mflMediaButton":
		case "settingRadioType":
		    prefVal = sPrefs.getString(key, "");
		    break;
		case "nightColorsWithInterior":
		case "navAvailable":
		case "stealthOneAvailable":
		    prefVal = (sPrefs.getBoolean(key, false)) ? "true" : "false";
		    break;
		case "timeUnit":
		case "distanceUnit":
		case "speedUnit":
		case "temperatureUnit":
		case "consumptionUnit":
		    prefVal = sPrefs.getString(key, "");
		    sendIBusCommand(
		        IBusCommand.Commands.GFXToIKESetUnits,
			Integer.parseInt(sPrefs.getString("speedUnit", "1")),
			Integer.parseInt(sPrefs.getString("distanceUnit", "1")),
			Integer.parseInt(sPrefs.getString("temperatureUnit", "1")),
			Integer.parseInt(sPrefs.getString("timeUnit", "0")),
			Integer.parseInt(sPrefs.getString("consumptionUnit", "01"))
		    );
		    break;
	    }
	    Log.d(
		TAG,
		String.format(
		    "%sGot preference change for %s -> %s", CTAG, key, prefVal
		)
	    );
	}
	
    };
    
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

    private void sendIBusCommand(
        final IBusCommand.Commands cmd, final Object... args
    ){
        if(getIBusLinkState()){
            mIBusService.sendCommand(new IBusCommand(cmd, args));
        }
    }
    
    private void showToast(String toastText){
        Context appContext = getActivity();
        Toast.makeText(appContext, toastText, Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onActivityCreated (Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, CTAG + "onActivityCreated()");
        mContext = getActivity();
        if(!mIBusConnected){
            serviceStarter(IBusMessageService.class, mIBusConnection);
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        addPreferencesFromResource(R.xml.settings_data);
    }
    
    @Override
    public View onCreateView(
	LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ){
        setRetainInstance(true);
        final View v = inflater.inflate(R.layout.settings, container, false);
        Log.d(TAG, CTAG + "onCreateView()");
        mSettings = getPreferenceManager().getSharedPreferences();
        mSettings.registerOnSharedPreferenceChangeListener(mSharedPrefListener);
        mOBCTime = (Preference) findPreference("obcTime");
        mOBCDate = (Preference) findPreference("obcDate");
	
        Preference syncCarDateTime = (Preference) findPreference(
	    "syncDateTime"
	);
        
        syncCarDateTime.setOnPreferenceClickListener(mPrefClickListener);
        return v;
    }
    
    @Override
    public void onResume(){
        super.onResume();
	Log.d(TAG, CTAG + "onResume()");
        mSettings.registerOnSharedPreferenceChangeListener(
	    mSharedPrefListener
	);
    }

    @Override
    public void onPause(){
        super.onPause();
	Log.d(TAG, CTAG + "onPause()");
        mSettings.unregisterOnSharedPreferenceChangeListener(
	    mSharedPrefListener
	);
    }
    
    @Override
    public void onDestroy(){
        super.onDestroy();
	mSettings.unregisterOnSharedPreferenceChangeListener(
	    mSharedPrefListener
	);
        mIBusService.unregisterCallback(mIBusCallbacks);
        Log.d(TAG, "Settings: onDestroy Called");
        if(mIBusConnected){
            mIBusService.unregisterCallback(mIBusCallbacks);
            serviceStopper(IBusMessageService.class, mIBusConnection);
        }
    }

}