package net.littlebigisland.droidibus.activity;

import net.littlebigisland.droidibus.R;
import net.littlebigisland.droidibus.ibus.IBusCommandsEnum;
import net.littlebigisland.droidibus.ibus.IBusCallbackReceiver;
import net.littlebigisland.droidibus.ibus.IBusMessageService;
import net.littlebigisland.droidibus.ibus.IBusMessageService.IOIOBinder;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class DashboardStatsFragment extends BaseFragment{
    
    protected Handler mHandler = new Handler();
    protected SharedPreferences mSettings = null;
    protected IBusMessageService mIBusService;
    // Keep track of the connection state
    protected boolean mIBusBound = false;
    
    // Views in the Activity
    protected RelativeLayout mDashboardLayout;
            
    // Fields in the activity
    protected TextView 
        mSpeedUnit, mAvgSpeedUnit, mRangeFieldUnit, mConsumption1Unit,
        mConsumption2Unit, mOutdoorTempUnit, mCoolantTempUnit,
        mSpeedField, mRPMField, mRangeField, mOutTempField,
        mCoolantTempField, mFuel1Field, mFuel2Field, mAvgSpeedField,
        mGeoCoordinatesField, mGeoStreetField, mGeoLocaleField,
        mGeoAltitudeField, mIKEDisplayField, mDateField, mTimeField;

    /** Service connection class for IBus
     */
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
                boardMonitorBootup(); 
                Log.d(TAG, "mIBusService connected and BoardMonitor Bootup Performed");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        	Log.d(TAG, "mIBusService disconnected");
            mIBusBound = false;
        }
    };
	
    /**
     * IBus Callback Functions
     */
    private IBusCallbackReceiver mIBusUpdateListener = new IBusCallbackReceiver() {
        private int mCurrentTextColor = R.color.dayColor;
        
        /** Callback to handle any vehicle speed updates
         * @param int Speed to set
         */
        @Override
        public void onUpdateSpeed(final int speed){
            if(mSettings.getString("speedUnit", "1").equals("0")){ // KM/h
                mSpeedField.setText(String.valueOf(speed));
            }else{// MPH
                mSpeedField.setText(String.valueOf((int) ((speed * 2) * 0.621371)));
            }
        }
		
        /** Callback to handle RPM updates
         * @param int RPM to set
         */
        @Override
        public void onUpdateRPM(final int rpm){
            mRPMField.setText(Integer.toString(rpm));
        }
        
        /** Callback to handle Fuel Range updates
         * @param String Range to set
         */
        @Override
        public void onUpdateRange(final String range){
            mRangeField.setText(range);
        }
        
        /** Callback to handle Outdoor Temperature updates
         * @param int Temperature to set
         */
        @Override
        public void onUpdateOutdoorTemp(final String temp){
            mOutTempField.setText(temp);
        }
        
        /** Callback to handle Coolant Temperature updates
         * @param int Temperature to set
         */
        @Override
        public void onUpdateCoolantTemp(final int temp){
            // Celsius 
            if(mSettings.getString("temperatureUnit", "1").equals("0")){
                mCoolantTempField.setText(Integer.toString(temp));
            }else{ // Freedom Units
                mCoolantTempField.setText(
                        "+" + Integer.toString(((temp * 9) / 5) + 32)
                );
            }
        }
		
        /** Callback to handle Fuel Consumption 1 Updates
         * @param String MPG to set
         */
        @Override
        public void onUpdateFuel1(final String mpg){
            mFuel1Field.setText(mpg);
        }
        
        /** Callback to handle Fuel Consumption 2 Updates
         * @param String MPG to set
         */
        @Override
        public void onUpdateFuel2(final String mpg){
            mFuel2Field.setText(mpg);
        }
		
        /** Callback to handle Average Speed Updates
         * @param String Speed to set
         */
        @Override
        public void onUpdateAvgSpeed(final String speed){
            mAvgSpeedField.setText(speed);
        }
        
        /** Callback to handle Time Updates
         * @param String Time to set
         */
        @Override
        public void onUpdateTime(final String time){
            mTimeField.setText(time);
        }
        
        /** Callback to handle Date Updates
         * @param String Date to set
         */
        @Override
        public void onUpdateDate(final String date){
            mDateField.setText(date);
        }

        /** Callback to handle updates from GPS about Street Location
         * @param String Street Name
         */
        @Override
        public void onUpdateStreetLocation(final String streetName) {
            mGeoStreetField.setText(streetName);
        }
        
        /** Callback to handle updates from GPS about Locale (City)
         * @param String City Name
         */
        @Override
        public void onUpdateLocale(final String cityName) {
            mGeoLocaleField.setText(cityName);
        }
        
        /** Callback to handle updates from GPS about coordinates
         * @param String GPS Coordinates
         */
        @Override
        public void onUpdateGPSCoordinates(final String gpsCoordinates){
            mGeoCoordinatesField.setText(gpsCoordinates);
        }
		
        /** Callback to handle updates from GPS about altitude
         * @param String GPS Altitude
         */
        @Override
        public void onUpdateGPSAltitude(final int altitude){
            mGeoAltitudeField.setText(String.format("%s'", (int) Math.round(altitude * 3.28084)));
        }
        
        /** Callback to handle updates from the LCM about interior lights
         * @param int Light status (0 or 1; on or off)
         */
        @Override
        public void onLightStatus(int lightStatus){
            if(mSettings.getBoolean("nightColorsWithInterior", false)){
                int color = (lightStatus == 1) ? R.color.nightColor : R.color.dayColor;
                // Only change the color if it's different
                if(color != mCurrentTextColor){
                    mCurrentTextColor = color;
                    changeTextColors(mDashboardLayout, color);
                }
            }
        }
        
        /** Callback to handle updates to the IKE units
         * @param String String array of 0s and 1s
         */
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
            updateDisplayedUnits();
        }

    };
	
    // Helper Methods
    /**
     * Emulate the messages the BoardMonitor sends when it boots up
     */
    private void boardMonitorBootup(){
        sendIBusCommand(IBusCommandsEnum.BMToGlobalBroadcastAliveMessage);
        sendIBusCommand(IBusCommandsEnum.BMToIKEGetIgnitionStatus);
        sendIBusCommand(IBusCommandsEnum.BMToLCMGetDimmerStatus);
        sendIBusCommand(IBusCommandsEnum.BMToGMGetDoorStatus);
        // Send a "get" request to populate the values on screen
        // Do it here because this is when the service methods come into scope
        sendIBusCommand(IBusCommandsEnum.BMToIKEGetTime);
        sendIBusCommand(IBusCommandsEnum.BMToIKEGetDate);
        sendIBusCommand(IBusCommandsEnum.BMToIKEGetFuel1);
        sendIBusCommand(IBusCommandsEnum.BMToIKEGetFuel2);
        sendIBusCommand(IBusCommandsEnum.BMToIKEGetOutdoorTemp);
        sendIBusCommand(IBusCommandsEnum.BMToIKEGetRange);
        sendIBusCommand(IBusCommandsEnum.BMToIKEGetAvgSpeed);
    }
	
    public void updateDisplayedUnits(){		
        // Consumption
        switch(mSettings.getString("consumptionUnit", "10")){
            case "00":
                mConsumption1Unit.setText("L/100");
                mConsumption2Unit.setText("L/100");
                break;
            case "01":
                mConsumption1Unit.setText("MPG");
                mConsumption2Unit.setText("MPG");
                break;
            case "11":
                mConsumption1Unit.setText("KM/L");
                mConsumption2Unit.setText("KM/L");
                break;
        }
        
        // Speed - Average Speed and Current Speed
        if(mSettings.getString("speedUnit", "1").equals("0")){
            mSpeedUnit.setText("KM/H");
            mAvgSpeedUnit.setText("KM/H");
        }else{
            mSpeedUnit.setText("MPH");
            mAvgSpeedUnit.setText("MPH");
        }
        // Distance
        if(mSettings.getString("distanceUnit", "1").equals("0")){
            mRangeFieldUnit.setText("Km");
        }else{
            mRangeFieldUnit.setText("Mi");
        }
        
        // Temperature
        if(mSettings.getString("temperatureUnit", "1").equals("0")){
            mOutdoorTempUnit.setText("C");
            mCoolantTempUnit.setText("C");
        }else{
            mOutdoorTempUnit.setText("F");
            mCoolantTempUnit.setText("F");
        }
    }
	
    // Android Methods Implemented Below
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.dashboard, container, false);
        Log.d(TAG, "Dashboard: onCreateView Called");
        
        // Load Activity Settings
        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        mSpeedUnit = (TextView) v.findViewById(R.id.speedFieldUnit);
        mAvgSpeedUnit = (TextView) v.findViewById(R.id.avgSpeedUnit);
        mRangeFieldUnit = (TextView) v.findViewById(R.id.rangeFieldUnit);
        mConsumption1Unit = (TextView) v.findViewById(R.id.consumption1Unit);
        mConsumption2Unit = (TextView) v.findViewById(R.id.consumption2Unit);
        mOutdoorTempUnit = (TextView) v.findViewById(R.id.outdoorTempUnit);
        mCoolantTempUnit = (TextView) v.findViewById(R.id.coolantTempUnit);

        // Set the settings
        updateDisplayedUnits();
    	
	    // Layouts
    	mDashboardLayout = (RelativeLayout) v.findViewById(R.id.dashboardLayout);
		
        // Setup the text fields for the view
        // OBC Fields
        mSpeedField = (TextView) v.findViewById(R.id.speedField);
        mRPMField = (TextView) v.findViewById(R.id.rpmField);
        mRangeField = (TextView) v.findViewById(R.id.rangeField);
        mFuel1Field = (TextView) v.findViewById(R.id.consumption1);
        mFuel2Field = (TextView) v.findViewById(R.id.consumption2);
        mAvgSpeedField = (TextView) v.findViewById(R.id.avgSpeed);
        
        // Temperature
        mOutTempField = (TextView) v.findViewById(R.id.outdoorTempField);
        mCoolantTempField = (TextView) v.findViewById(R.id.coolantTempField);
        
        // Geo Fields
        mGeoCoordinatesField = (TextView) v.findViewById(R.id.geoCoordinatesField);
        mGeoStreetField = (TextView) v.findViewById(R.id.geoStreetField);
        mGeoLocaleField = (TextView) v.findViewById(R.id.geoLocaleField);
        mGeoAltitudeField = (TextView) v.findViewById(R.id.geoAltitudeField);
        
        // IKE Display Field (Currently for StealthOne Support)
        mIKEDisplayField = (TextView) v.findViewById(R.id.ikeDisplayField);
                
        // Time & Date Fields
        mDateField = (TextView) v.findViewById(R.id.dateField);
        mTimeField = (TextView) v.findViewById(R.id.timeField);

        // Set the long press of values for IKE resets
        OnLongClickListener valueResetter = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                IBusCommandsEnum action = IBusCommandsEnum.valueOf(v.getTag().toString());
                switch(action){
                    case BMToIKEResetFuel1:
                        showToast("Resetting Fuel 1 Value");
                        break;
                    case BMToIKEResetFuel2:
                        showToast("Resetting Fuel 2 Value");
                        break;
                    case BMToIKEResetAvgSpeed:
                        showToast("Resetting Average Speed Value");
                        break;
                    default:
                            break;
                }
                sendIBusCommand(action);
                return true;
            }
        };
        
        mFuel1Field.setTag(IBusCommandsEnum.BMToIKEResetFuel1.name());
        mFuel2Field.setTag(IBusCommandsEnum.BMToIKEResetFuel1.name());
        mAvgSpeedField.setTag(IBusCommandsEnum.BMToIKEResetAvgSpeed.name());

        mFuel1Field.setOnLongClickListener(valueResetter);
        mFuel2Field.setOnLongClickListener(valueResetter);
        mAvgSpeedField.setOnLongClickListener(valueResetter);
		
		
        // Act on preferences
        if(!mSettings.getBoolean("navAvailable", false)){
            int layoutMargin =  (int) (230 * getResources().getDisplayMetrics().density + 0.5f);
            View geoLayout = v.findViewById(R.id.geoLayout);
            View geoLocation = v.findViewById(R.id.geoLocation);
            geoLayout.setVisibility(View.GONE);
            geoLocation.setVisibility(View.GONE);
            // Edit the geometry of the adjacent items for fit
            LinearLayout.LayoutParams tempLayoutParams = (
                LinearLayout.LayoutParams
            ) v.findViewById(R.id.tempLayout).getLayoutParams();
            tempLayoutParams.setMargins(layoutMargin, 0, 0, 0);
            
            LinearLayout.LayoutParams consumptionLayoutParams = (
                LinearLayout.LayoutParams
            ) v.findViewById(R.id.consumptionLayout).getLayoutParams();
            consumptionLayoutParams.setMargins(layoutMargin, 0, 0, 0);
                
        }
        
	    if(!mSettings.getBoolean("stealthOneAvailable", false)){
	        mIKEDisplayField.setVisibility(View.GONE);
	    }
	    
        return v;
    }
	
    @Override
    public void onActivityCreated (Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated Called");
        // Bind required background services last since the callback
        // functions depend on the view items being initialized
        if(!mIBusBound){
            serviceStarter(IBusMessageService.class, mIBusConnection);
        }
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	Log.d(TAG, "Dashboard: onResume called");
    	if(mIBusBound){
            Log.d(TAG, "Dashboard: IOIO bound in onResume");
            if(!mIBusService.getLinkState()){
                serviceStopper(IBusMessageService.class, mIBusConnection);
                serviceStarter(IBusMessageService.class, mIBusConnection);
                Log.d(TAG, "IOIO Bound but not connected in onResume");
            }
    	}else{
            Log.d(TAG, "IOIO NOT bound in onResume");
            serviceStarter(IBusMessageService.class, mIBusConnection);
    	}
        // Send BoardMonitor Get requests to populate blank view
        boardMonitorBootup();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d(TAG, "Dashboard: onDestroy called");
    	mIBusService.removeCallback(mIBusUpdateListener);
    	if(mIBusBound){
    		mIBusService.disable();
    		serviceStopper(IBusMessageService.class, mIBusConnection);
    	}
    }
}