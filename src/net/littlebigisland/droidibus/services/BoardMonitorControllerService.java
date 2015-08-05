package net.littlebigisland.droidibus.services;

import java.util.Calendar;

import net.littlebigisland.droidibus.ibus.IBusCommand;
import net.littlebigisland.droidibus.ibus.IBusSystem;
import net.littlebigisland.droidibus.resources.ServiceManager;
import net.littlebigisland.droidibus.resources.ThreadExecutor;
import net.littlebigisland.droidibus.resources.enums.IgnitionStates;
import net.littlebigisland.droidibus.resources.enums.MFLPlaybackModes;
import net.littlebigisland.droidibus.resources.enums.RadioModes;
import net.littlebigisland.droidibus.resources.enums.RadioTypes;
import net.littlebigisland.droidibus.services.IBusMessageService.IOIOBinder;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class BoardMonitorControllerService extends Service{
    
    protected static final String TAG = "DroidIBus: ";
    protected static final String CTAG = "RadioControllerService: ";
    
    protected Handler mHandler = new Handler();
    protected Context mContext = null;
    
    protected SharedPreferences mSettings = null;
    
    protected ThreadExecutor mThreadExecutor = new ThreadExecutor();
    
    protected IBusMessageService mIBusService = null;
    protected boolean mIBusConnected = false;
    
    protected MusicControllerService mPlayerService;
    protected boolean mMediaPlayerConnected = false;
    
    // Application State Variables
    protected RadioTypes mRadioType = RadioTypes.BM53;
    protected RadioModes mRadioMode = RadioModes.AUX;
    protected IgnitionStates mIgnitionState = IgnitionStates.OFF;
    protected MFLPlaybackModes mMFLPlaybackMode = null;
    
    protected boolean mRadioModeSynced = true;
    protected boolean mWasMediaPlaying = false;
    protected boolean mCompactDiscPlaying = false;
    
    public abstract class RadioControllerCallbacks{
        
        public void onUpdateRadioMode(final RadioModes radioMode){}
        
        public void onUpdateRadioText(final String radioText){}
        
        public void onTogglePlayback(){}
        
    }
    
    // Service connection class for IBus
    private ServiceConnection mIBusConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            mIBusService = ((IOIOBinder) service).getService();
            Log.d(TAG, CTAG + "IBus service connected");
            mIBusConnected = true;
            mIBusService.registerCallback(mIBusCallbacks, mHandler);
            if(mRadioType == RadioTypes.BM53){
                Log.d(TAG, CTAG + "Starting mRadioUpdater");
                //mThreadExecutor.execute(mRadioUpdater);
                mHandler.postDelayed(mSendInfoButton, 2500);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name){
            Log.d(TAG, CTAG + "IBus service disconnected");
            mIBusConnected = false;
        }
        
    };
    
    private ServiceConnection mPlayerConnection = new ServiceConnection(){
        
        @Override
        public void onServiceConnected(ComponentName className, IBinder service){
            Log.d(TAG, CTAG + "MusicControllerService Connected");
            // Getting the binder and activating RemoteController instantly
            MusicControllerService.MusicControllerBinder serviceBinder = (
                MusicControllerService.MusicControllerBinder
            ) service;
            mPlayerService = serviceBinder.getService();
            mMediaPlayerConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name){
            Log.d(TAG, CTAG + "MusicControllerService Disconnected");
            mMediaPlayerConnected = false;
        }

    };
    
    private IBusSystem.Callbacks mIBusCallbacks = new IBusSystem.Callbacks(){
                
        /**
        * Callback to handle any updates to the station text when in Radio Mode
        * @param text Text to set
        */
        @Override
        public void onUpdateRadioStation(final String text){
            if(mRadioType != RadioTypes.BM53){
                return;
            }
            // If this is a BM53 unit, we should listen for
            // Updates to the station text
            if(text.contains("CD") || text.contains("TR")){
                mRadioMode = RadioModes.CD;
            }else{
                switch(text){
                    case "TAPE A":
                    case "TAPE B":
                        mRadioMode = RadioModes.TAPE;
                        break;
                    case "AUX":
                        mRadioMode = RadioModes.AUX;
                        break;
                    default:
                        mRadioMode = RadioModes.Radio;
                        break;
                }
            }
            // Sync modes with the car
            // Make sure we're not changing modes
            /*
            if(mRadioModeSynced){
                int auxMode = mTabletLayout.getVisibility();
                if(mRadioMode == RadioModes.AUX && auxMode == View.GONE){
                    Log.d(TAG, CTAG + "Toggle to radio");
                    mBtnMusicMode.toggle();
                }
                if(mRadioMode != RadioModes.AUX && auxMode == View.VISIBLE){
                    Log.d(TAG, CTAG + "Toggle to AUX");
                    mBtnMusicMode.toggle();
                }
            }
            mStationText.setText(text);
            */
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
                if(mMediaPlayerConnected && mRadioMode == RadioModes.AUX){
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
                if(mMediaPlayerConnected){
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
            if(mMediaPlayerConnected){
                mPlayerService.skipToNext();
            }
        }
        
        @Override
        public void onTrackPrev(){
            if(mMediaPlayerConnected){
                mPlayerService.skipToPrevious();
            }
        }
        
        @Override
        public void onVoiceBtnPress(){
            // Re-purpose this button to pause/play music
            if(mMFLPlaybackMode == MFLPlaybackModes.PRESS && mMediaPlayerConnected){
                mPlayerService.togglePlayback();
            }
        }
        
        @Override
        public void onVoiceBtnHold(){
            // Re-purpose this button to pause/play music
            if(mMFLPlaybackMode == MFLPlaybackModes.HOLD && mMediaPlayerConnected){
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
            sendIBusCommand(
                IBusCommand.Commands.BMToRadioCDStatus, 0, trackAndCD, trackAndCD
            );
            if(mCompactDiscPlaying){
                sendIBusCommand(
                    IBusCommand.Commands.BMToRadioCDStatus, 1, trackAndCD, trackAndCD
                );
            }else{
                sendIBusCommand(
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
    @SuppressWarnings("unused")
    private Runnable mRadioUpdater = new Runnable(){

        private static final int mTimeout = 5000;
        private long mLastUpdate = 0;
        
        @Override
        public void run(){
            Log.d(TAG, "mRadioUpdater is running");
            while(!Thread.currentThread().isInterrupted()){
                long timeNow = getTimeNow();
                if(getIBusLinkState() && (timeNow - mLastUpdate) >= mTimeout){
                    mLastUpdate = timeNow;
                    sendIBusCommand(IBusCommand.Commands.BMToRadioGetStatus);
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
        @Override
        public void run(){
            sendPressReleaseCommand("BMToRadioInfo");
        }
    };
    
    public void setRadioMode(final RadioModes desiredMode){
        if(mRadioType == RadioTypes.BM53 && getIBusLinkState()){
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
    
    /**
     * Get the IBus link state - Useful to check if we can write to the bus
     * @return boolean IBus link state
     */
    public boolean getIBusLinkState(){
        if(mIBusConnected){
            if(mIBusService != null){
                return mIBusService.getLinkState();
            }else{
                Log.e(TAG, CTAG + "mIBusService is null!");
            }
        }
        return false;
    }
    
    private long getTimeNow(){
        return Calendar.getInstance().getTimeInMillis();
    }
    
    private void sendIBusCommand(IBusCommand.Commands cmd, final Object... args){
        if(mIBusConnected){
            mIBusService.sendCommand(new IBusCommand(cmd, args));
        }else{
            Log.e(
               TAG, 
               String.format("IBusService unbound: Discarding command %s", cmd)
            );
        }
    }
    
    private void sendIBusCommandDelayed(final IBusCommand.Commands cmd, 
            final long delayMils, final Object... args){
        mHandler.postDelayed(new Runnable(){
            public void run(){
                sendIBusCommand(cmd, args);
            }
        }, delayMils);
    }
    
    private void sendPressReleaseCommand(String cmdName){
        IBusCommand.Commands cmdPress = IBusCommand.Commands.valueOf(
            cmdName + "Press"
        );
        IBusCommand.Commands cmdRelease = IBusCommand.Commands.valueOf(
            cmdName + "Release"
        );
        if(cmdPress != null && cmdRelease != null){
            sendIBusCommand(cmdPress);
            sendIBusCommandDelayed(cmdRelease, 250);
        }else{
            Log.e(TAG, CTAG + "Error sending unknown IBus Command " + cmdName);
        }
    }
    
    @SuppressWarnings("rawtypes")
    protected void serviceStarter(Class cls, ServiceConnection svcConn){
        boolean res = ServiceManager.startService(cls, svcConn, mContext);
        if(res){
            Log.d(TAG, CTAG + "Starting " + cls.toString());
        }else{
            Log.e(TAG, CTAG + "Unable to start " + cls.toString());
        }
    }
    
    @SuppressWarnings("rawtypes")
    protected void serviceStopper(Class cls, ServiceConnection svcConn){
        boolean res = ServiceManager.stopService(cls, svcConn, mContext);
        if(res){
            Log.d(TAG, CTAG + "Unbinding from " + cls.toString());
        }else{
            Log.e(TAG, CTAG + "Unable to unbind from " + cls.toString());
        }
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
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
        if(!mMediaPlayerConnected){
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
        if(mMediaPlayerConnected){
            serviceStopper(MusicControllerService.class, mPlayerConnection);
        }
    }
}
