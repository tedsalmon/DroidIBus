package net.littlebigisland.droidibus.services;

import net.littlebigisland.droidibus.ibus.IBusCommand;
import net.littlebigisland.droidibus.ibus.IBusSystem;
import net.littlebigisland.droidibus.resources.IBusMessageServiceConnection;
import net.littlebigisland.droidibus.resources.MusicControllerServiceConnection;
import net.littlebigisland.droidibus.resources.ServiceManager;
import net.littlebigisland.droidibus.resources.ThreadExecutor;
import net.littlebigisland.droidibus.resources.TimeHelper;
import net.littlebigisland.droidibus.resources.enums.IgnitionStates;
import net.littlebigisland.droidibus.resources.enums.MFLPlaybackModes;
import net.littlebigisland.droidibus.resources.enums.RadioModes;
import net.littlebigisland.droidibus.resources.enums.RadioTypes;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class IBusControllerService extends Service{
    
    protected static final String TAG = "DroidIBus: ";
    protected static final String CTAG = "RadioControllerService: ";
    
    protected Handler mHandler = new Handler();
    protected Context mContext = null;
    private IBinder mBinder = new IBusControllerBinder();
    
    protected SharedPreferences mSettings = null;
    
    protected ThreadExecutor mThreadExecutor = new ThreadExecutor();
    
    protected IBusMessageService mIBusService = null;
    protected boolean mIBusConnected = false;
    
    protected MusicControllerService mPlayerService;
    
    // Application State Variables
    protected RadioTypes mRadioType = RadioTypes.BM53;
    protected RadioModes mRadioMode = RadioModes.AUX;
    protected IgnitionStates mIgnitionState = IgnitionStates.OFF;
    protected MFLPlaybackModes mMFLPlaybackMode = null;
    
    protected boolean mRadioModeSynced = true;
    protected boolean mWasMediaPlaying = false;
    protected boolean mCompactDiscPlaying = false;
    
    protected long mLastRDSMessage = 0;
    
    /**
     * Return the IBusControllerService on bind
     * @return MusicControllerService instance
     */
    public class IBusControllerBinder extends Binder{
        public IBusControllerService getService(){
            return IBusControllerService.this;
        }
    }
    
    public abstract class RadioControllerCallbacks{
        
        public void onUpdateRadioMode(final RadioModes radioMode){}
        
        public void onUpdateRadioText(final String radioText){}
        
        public void onTogglePlayback(){}
        
    }
    
    private IBusMessageServiceConnection mIBusConnection = 
            new IBusMessageServiceConnection(){
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            super.onServiceConnected(name, service);
            mIBusService.registerCallback(mIBusCallbacks, mHandler);
            if(mRadioType == RadioTypes.BM53){
                mThreadExecutor.execute(mRadioUpdater);
                mHandler.postDelayed(mSendInfoButton, 2500);
            }
            sendBoardMonitorBootup();
        }
        
    };
    
    private MusicControllerServiceConnection mPlayerConnection = 
            new MusicControllerServiceConnection(){
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            super.onServiceConnected(name, service);
            mPlayerService = getService();
        }
    };
    
    private IBusSystem.Callbacks mIBusCallbacks = new IBusSystem.Callbacks(){
                
        /**
        * Callback to handle any updates to the station text when in Radio Mode
        * @param text Text to set
        */
        @Override
        public void onUpdateRadioStation(final String text){
            mLastRDSMessage = TimeHelper.getTimeNow();
            if(mRadioType != RadioTypes.BM53){
                return;
            }
            RadioModes currMode = null;
            // If this is a BM53 unit, we should listen for
            // Updates to the station text
            if(text.contains("CD") || text.contains("TR")){
                currMode = RadioModes.CD;
            }else{
                switch(text){
                    case "TAPE A":
                    case "TAPE B":
                        currMode = RadioModes.TAPE;
                        break;
                    case "AUX":
                        currMode = RadioModes.AUX;
                        break;
                    default:
                        currMode = RadioModes.Radio;
                        break;
                }
            }
            if(currMode != mRadioMode){
                mRadioMode = currMode;
                // Send Update callback
            }
            // Send Text Update Callback
        }
        
        /** Callback to handle Ignition state updates
        * @param int Current Ignition State
        */
        @Override
        public void onUpdateIgnitionSate(final int state) {
            boolean ignitionOn = (state > 0) ? true : false; 
            if(ignitionOn){
                if(mIgnitionState == IgnitionStates.OFF){
                    // Send info request to verify the BM53 is on the RDS screen
                    mHandler.postDelayed(mSendInfoButton, 5000);
                }
                if(mPlayerConnection.isConnected() && mRadioMode == RadioModes.AUX){
                    if(!mPlayerService.getIsPlaying() && mWasMediaPlaying){
                        mHandler.postDelayed(new Runnable(){
                            @Override
                            public void run(){
                                mPlayerService.play();
                            }
                        }, 5000);
                        mWasMediaPlaying = false;
                    }
                }
            }else{
                if(mPlayerConnection.isConnected()){
                    if(mPlayerService.getIsPlaying()){
                        mPlayerService.pause();
                        mWasMediaPlaying = false;
                    }
                }
            }
            mIgnitionState = (ignitionOn) ? IgnitionStates.ON : IgnitionStates.OFF;
        }
        
        @Override
        public void onTrackFwd(){
            if(mPlayerConnection.isConnected()){
                mPlayerService.skipToNext();
            }
        }
        
        @Override
        public void onTrackPrev(){
            if(mPlayerConnection.isConnected()){
                mPlayerService.skipToPrevious();
            }
        }
        
        @Override
        public void onVoiceBtnPress(){
            // Re-purpose this button to pause/play music
            if(mMFLPlaybackMode == MFLPlaybackModes.PRESS && mPlayerConnection.isConnected()){
                mPlayerService.togglePlayback();
            }
        }
        
        @Override
        public void onVoiceBtnHold(){
            // Re-purpose this button to pause/play music
            if(mMFLPlaybackMode == MFLPlaybackModes.HOLD && mPlayerConnection.isConnected()){
                mPlayerService.togglePlayback();
            }
        }
        
        @Override
        public void onUpdateRadioStatus(int status){
            // Radio is off, turn it on
            if(status == 0){
                sendPressReleaseCommand("BMToRadioPwr");
            }
        }
        
        @Override
        public void onRadioCDStatusRequest(){
            // Tell the Radio we have a CD on track 1
            byte trackAndCD = (byte) 0x01;
            mIBusConnection.sendCommand(
                IBusCommand.Commands.BMToRadioCDStatus, 0, trackAndCD, trackAndCD
            );
            if(mCompactDiscPlaying){
                mIBusConnection.sendCommand(
                    IBusCommand.Commands.BMToRadioCDStatus, 1, trackAndCD, trackAndCD
                );
            }else{
                mIBusConnection.sendCommand(
                    IBusCommand.Commands.BMToRadioCDStatus, 0, trackAndCD, trackAndCD
                );
            }
        }
        
        @Override
        public void onUpdateScreenState(int state){
            // Screen state 01 and 02
            switch((byte) state){
                case 0x01: // Gracefully went home menu
                case 0x02: // Timed out to home menu
                    mHandler.postDelayed(mSendInfoButton, 150);
                    break;
            }
        }

    };
    
    /**
     *  This thread should make sure to send out and request
     *  any IBus messages that the BM usually would.
     *  We should also keep the radio on "Info" mode at all times here.
     */
    private Runnable mRadioUpdater = new Runnable(){

        private static final int mTimeout = 5000;
        private long mLastUpdate = 0;
        
        @Override
        public void run(){
            Log.d(TAG, "mRadioUpdater is running");
            while(!Thread.currentThread().isInterrupted()){
                long timeNow = TimeHelper.getTimeNow();
                if(mIBusConnection.getLinkState() && (timeNow - mLastUpdate) >= mTimeout){
                    mLastUpdate = timeNow;
                    mIBusConnection.sendCommand(IBusCommand.Commands.BMToRadioGetStatus);
                }
                try{
                    Thread.sleep(mTimeout);
                }catch(InterruptedException e){
                    Log.e(TAG, CTAG + "mRadioUpdater InterruptedException");
                }
            }
            Log.d(TAG, "mRadioUpdater is returning");
            return;
        }
    };
    
    private Runnable mSendInfoButton = new Runnable(){
        private static final int TIMEOUT = 1000;
        private boolean mFirstRun = true;
        @Override
        public void run(){
            sendPressReleaseCommand("BMToRadioInfo");
            if((TimeHelper.getTimeNow() - mLastRDSMessage) > TIMEOUT || mFirstRun){
                mHandler.postDelayed(this, TIMEOUT);
                mFirstRun = false;
            }else{
                mFirstRun = true;
                mHandler.removeCallbacks(this);
            }
        }
    };
    
    public void setRadioMode(final RadioModes desiredMode){
        if(mRadioType == RadioTypes.BM53 && mIBusConnection.getLinkState()){
            mRadioModeSynced = false;
            mThreadExecutor.execute(new Runnable(){
                
                @Override
                public void run(){
                    while(desiredMode != mRadioMode && !mRadioModeSynced){
                        Log.d(TAG, 
                            String.format(
                                CTAG + "Radio Mode: %s -> %s", mRadioMode.toString(),
                                desiredMode.toString()
                            )
                        );
                        sendPressReleaseCommand("BMToRadioMode");
                        try{
                            Thread.sleep(1000);
                        }catch (InterruptedException e){
                            Log.e(TAG, CTAG + "setRadioMode InterruptedException");
                        }
                        if(desiredMode == mRadioMode){
                            mRadioModeSynced = true;
                        }
                    }
                    mRadioModeSynced = true;
                    return;
                }

            });
        }
    }
    
    public void sendBoardMonitorBootup(){
        mIBusConnection.sendCommand(IBusCommand.Commands.BMToGlobalBroadcastAliveMessage);
        mIBusConnection.sendCommand(IBusCommand.Commands.GFXToIKEGetIgnitionStatus);
        mIBusConnection.sendCommand(IBusCommand.Commands.BMToLCMGetDimmerStatus);
        mIBusConnection.sendCommand(IBusCommand.Commands.BMToGMGetDoorStatus);
        mIBusConnection.sendCommand(IBusCommand.Commands.GFXToIKEGetTime);
        mIBusConnection.sendCommand(IBusCommand.Commands.GFXToIKEGetDate);
        mIBusConnection.sendCommand(IBusCommand.Commands.GFXToIKEGetFuel1);
        mIBusConnection.sendCommand(IBusCommand.Commands.GFXToIKEGetFuel2);
        mIBusConnection.sendCommand(IBusCommand.Commands.GFXToIKEGetOutdoorTemp);
        mIBusConnection.sendCommand(IBusCommand.Commands.GFXToIKEGetRange);
        mIBusConnection.sendCommand(IBusCommand.Commands.GFXToIKEGetAvgSpeed);
    }
    
    private void sendPressReleaseCommand(String cmdName){
        IBusCommand.Commands cmdPress = IBusCommand.Commands.valueOf(
            cmdName + "Press"
        );
        IBusCommand.Commands cmdRelease = IBusCommand.Commands.valueOf(
            cmdName + "Release"
        );
        if(cmdPress != null && cmdRelease != null){
            mIBusConnection.sendCommand(cmdPress);
            mIBusConnection.sendCommandDelayed(cmdRelease, 250);
        }else{
            Log.e(TAG, CTAG + "Error sending unknown IBus Command " + cmdName);
        }
    }
    
    @SuppressWarnings("rawtypes")
    protected void serviceStarter(Class cls, ServiceConnection svcConn){
        ServiceManager.startService(cls, svcConn, mContext);
    }
    
    @SuppressWarnings("rawtypes")
    protected void serviceStopper(Class cls, ServiceConnection svcConn){
        ServiceManager.stopService(cls, svcConn, mContext);
    }

    @Override
    public IBinder onBind(Intent intent){
        return mBinder;
    }
    
    @Override
    public void onCreate(){
        Log.d(TAG, CTAG + "onCreate()");
        // Saving the context for further reuse
        mContext = getApplicationContext();
        mSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
        if(!mIBusConnected){
            serviceStarter(IBusMessageService.class, mIBusConnection);
        }
        if(!mPlayerConnection.isConnected()){
            serviceStarter(MusicControllerService.class, mPlayerConnection);
        }
    }
    
    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, CTAG + "onDestroy()");
        if(mIBusConnected){
            mIBusService.unregisterCallback(mIBusCallbacks);
            serviceStopper(IBusMessageService.class, mIBusConnection);
        }
        if(mPlayerConnection.isConnected()){
            serviceStopper(MusicControllerService.class, mPlayerConnection);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStartCommand()");
        return START_STICKY;
    }
}
