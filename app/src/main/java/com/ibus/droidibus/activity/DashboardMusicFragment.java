package com.ibus.droidibus.activity;

/**
 * Dashboard Music Fragment - Controls Music Player 
 * and Media functionality to the IBus device
 * @author Ted <tass2001@gmail.com>
 * @package com.ibus.droidibus.activity
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.ibus.droidibus.R;
import com.ibus.droidibus.ibus.IBusCommand;
import com.ibus.droidibus.ibus.IBusMessageService;
import com.ibus.droidibus.ibus.IBusSystem;
import com.ibus.droidibus.music.MusicControllerService;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;


public class DashboardMusicFragment extends BaseFragment{

    protected String CTAG = "DashboardMusicFragment: ";
    protected SharedPreferences mSettings = null;
    
    protected static final int CONTROLLER_UPDATE_RATE = 1000;
    protected static final int RADIO_UPDATE_RATE = 3000;
    protected static final int SEEKBAR_UPDATE_RATE = 250;
    
    private static final int ARTWORK_HEIGHT = 114;
    private static final int ARTWORK_WIDTH = 114;
    
    // Fields in the activity
    protected TextView mStationText, mRDSField, mProgramField,
        mBroadcastField, mStereoField;

    // Views in the Activity
    protected LinearLayout mRadioLayout, mTabletLayout, mMetaDataLayout;
    protected ImageButton mPlayerPrevBtn, mPlayerControlBtn, mPlayerNextBtn;
    protected TextView mPlayerArtistText, mPlayerTitleText, mPlayerAlbumText;
    protected SeekBar mPlayerScrubBar;
    protected ImageView mPlayerArtwork;
    protected Switch mBtnMusicMode;
    protected Spinner mMediaSessionSelector;
    
    protected PackageManager mPackageManager = null;
    
    ArrayAdapter<String> mMediaControllerSelectorAdapter = null;
    ArrayList<String> mMediaControllerSelectorList = new ArrayList<String>();
    HashMap<String,String> mMediaControllerNames = new HashMap<String,String>();
    
    protected MusicControllerService mPlayerService;
    protected boolean mMediaPlayerConnected = false;

    protected boolean mIsPlaying = false;
    protected boolean mWasPlaying = false;
    protected long mSongDuration = 1;
    protected boolean mCanSeek = true;

    protected RadioModes mRadioMode = RadioModes.AUX;
    protected RadioTypes mRadioType = RadioTypes.BM53;
    protected long mLastInfoRequest = 0;
    protected boolean mRadioModeSatisfied = true;
    protected boolean mCDPlayerPlaying = false;
    
    protected MFLPlaybackModes mMFLPlaybackMode = null;
    
    protected IgnitionStates mIgnitionState = IgnitionStates.OFF;

    private enum IgnitionStates{
        OFF, ON
    }
    
    private enum RadioModes{
        AUX, TAPE, CD, Radio
    }
    
    private enum RadioTypes{
        BM53, CD53, C43
    }
    
    private enum MFLPlaybackModes{
        PRESS, HOLD, DISABLED;
    }

    // Service connection class for IBus
    private IBusServiceConnection mIBusConnection = new IBusServiceConnection(){
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            super.onServiceConnected(name, service);
            registerIBusCallback(mIBusCallbacks, mHandler);
            Log.d(TAG, CTAG + "IBus Service Connected");

            if(mRadioType == RadioTypes.BM53){
                Log.d(TAG, CTAG + "Starting mRadioUpdater");
                mThreadExecutor.execute(mRadioUpdater);
                mHandler.postDelayed(mSendInfoButton, 2500);
            }
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
            mPlayerService.registerCallback(mPlayerCallbacks, mHandler);
            
            setMediaMetadata(mPlayerService.getMediaMetadata());
            setPlaybackState(mPlayerService.getMediaPlaybackState());
            
            mThreadExecutor.execute(mSeekbarUpdater);

            mHandler.post(mUpdateAvailableControllers);
        }

        @Override
        public void onServiceDisconnected(ComponentName name){
            Log.d(TAG, CTAG + "MusicControllerService Disconnected");
            mHandler.removeCallbacks(mUpdateAvailableControllers);
            mMediaPlayerConnected = false;
        }

    };
    
    private OnTouchListener mCommandTouchListener = new OnTouchListener(){
        
        @Override
        public boolean onTouch(View v, MotionEvent event){
            IBusCommand.Commands cmd = null;
            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    cmd = IBusCommand.Commands.valueOf(
                        v.getTag().toString() + "Press"
                    );
                    break;
                case MotionEvent.ACTION_UP:
                    cmd = IBusCommand.Commands.valueOf(
                        v.getTag().toString() + "Release"
                    );
                    break;
            }
            if(cmd != null){
                sendIBusCommand(cmd);
            }
            v.performClick();
            return false;
        }
        
    };
    
    private OnTouchListener mMetadataTouchListener = new OnTouchListener(){

        @Override
        public boolean onTouch(View v, MotionEvent event){
            MediaController mc = mPlayerService.getMediaController();
            if(mc != null){
                startActivity(
                    mPackageManager.getLaunchIntentForPackage(
                        mc.getPackageName()
                    )
                );
            }
            v.performClick();
            return false;
        }
        
    };
    
    private OnClickListener mPlaybackClickListener = new OnClickListener(){
        @Override
        public void onClick(View v){
            switch(v.getId()) {
                case R.id.playerPrevBtn:
                    mPlayerService.skipToPrevious();
                    break;
                case R.id.playerNextBtn:
                    mPlayerService.skipToNext();
                    break;
                case R.id.playerPlayPauseBtn:
                    togglePlayback();
                    break;
            }
        }
    };
    
    private OnClickListener mCommandClickListener = new OnClickListener(){
        @Override
        public void onClick(View v){
            sendIBusCommand((IBusCommand.Commands) v.getTag());
        }
    };
    
    private OnCheckedChangeListener mModeChangeListener = new OnCheckedChangeListener(){
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
            Log.d(TAG, CTAG + "Changing Music Mode");
            // Tablet Mode if checked, else Radio
            if(isChecked){
                // Send IBus Message
                changeRadioMode(RadioModes.AUX);
                mRadioLayout.setVisibility(View.GONE);
                mTabletLayout.setVisibility(View.VISIBLE);
                mMediaSessionSelector.setVisibility(View.VISIBLE);
            }else{
                if(mIsPlaying){
                    mPlayerService.pause();
                }
                // Send IBus Message
                changeRadioMode(RadioModes.Radio);
                mRadioLayout.setVisibility(View.VISIBLE);
                mTabletLayout.setVisibility(View.GONE);
                mMediaSessionSelector.setVisibility(View.GONE);
            }
        }
    };
    
    private OnSeekBarChangeListener mSeekbarChangeListener = new OnSeekBarChangeListener(){
        private float mSeekPosition = 0; 
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
            if(mMediaPlayerConnected && fromUser){
                mSeekPosition = mSongDuration * ((float)progress / (float)seekBar.getMax());
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar){
            mCanSeek = false;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mPlayerService.seekTo((long)mSeekPosition);
            mCanSeek = true;
        }
    };
    
    private OnItemSelectedListener mMediaSessionSelectedListener = new OnItemSelectedListener(){
        
        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                int position, long id) {
            clearMusicPlayerView();
            String remoteName = mMediaControllerNames.get(
                (String) mMediaSessionSelector.getSelectedItem()
            );
            Log.d(TAG, CTAG + "Setting session to " + remoteName);
            mMediaSessionSelector.setSelection(position);
            mPlayerService.setMediaSession(remoteName);
            
            setMediaMetadata(mPlayerService.getMediaMetadata());
            setPlaybackState(mPlayerService.getMediaPlaybackState());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent){}
        
    };
    
    private Runnable mUpdateAvailableControllers = new Runnable(){
        @Override
        public void run(){
            if(mMediaPlayerConnected){
                setMediaControllerSelection(mPlayerService.getMediaSessions());
            }
            mHandler.postDelayed(this, CONTROLLER_UPDATE_RATE);
        }
    };
    
    private Runnable mSeekbarUpdater = new Runnable(){
        
        int mProgress = 0;
        boolean mShouldRun = true;
        Runnable mSendUpdate = new Runnable(){
            @Override
            public void run(){
                mPlayerScrubBar.setProgress(mProgress);
            }
         };

        @Override
        public void run(){
            while(mShouldRun && !Thread.currentThread().isInterrupted()){
                PlaybackState pbState = mPlayerService.getMediaPlaybackState();
                if(pbState != null && mCanSeek){
                    mProgress = (int) pbState.getPosition();
                    mHandler.post(mSendUpdate);
                }
                try{
                    Thread.sleep(SEEKBAR_UPDATE_RATE);
                }catch(InterruptedException e){
                    Log.e(TAG, CTAG + "mUpdateSeekBar InterruptedException");
                    mShouldRun = false;
                }
            }
            return;
        }
        
    };
    
    private Runnable mSendInfoButton = new Runnable(){
        @Override
        public void run(){
            sendPressReleaseCommand("BMToRadioInfo");
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
    
    private MediaController.Callback mPlayerCallbacks = new MediaController.Callback(){
        
        @Override
        public void onMetadataChanged(MediaMetadata metadata){
            Log.d(TAG, CTAG + "Callback onMetadataChanged()");
            setMediaMetadata(metadata);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state){
            Log.d(TAG,  CTAG + "Callback onPlaybackStateChanged()");
            setPlaybackState(state.getState());
        }

        @Override
        public void onSessionDestroyed(){
            Log.d(TAG,  CTAG + "Callback onSessionDestroyed()");
            clearMusicPlayerView();
        }

    };
    
    private IBusSystem.Callbacks mIBusCallbacks = new IBusSystem.Callbacks(){
    	
    	private int mCurrentTextColor = R.color.dayColor;
		
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
            switch(text){
                case "NO CD":
                case "CD 1-04":
                case "TR 01":
                    mRadioMode = RadioModes.CD;
                    break;
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
            // Sync modes with the car
            // Make sure we're not changing modes
            if(mRadioModeSatisfied){
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
        }
		
    	@Override
        public void onUpdateRadioBrodcasts(String broadcastType){
            mBroadcastField.setText(broadcastType);
        }

        @Override
        public void onUpdateRadioStereoIndicator(String stIndicator){
            int vis = (stIndicator.equals("")) ? View.GONE : View.VISIBLE;
            mStereoField.setVisibility(vis);
        }

        @Override
        public void onUpdateRadioRDSIndicator(String rdsIndicator){
            int vis = (rdsIndicator.equals("")) ? View.GONE : View.VISIBLE;
            mRDSField.setVisibility(vis);
        }

        @Override
        public void onUpdateRadioProgramIndicator(final String currentProgram){
            mProgramField.setText(currentProgram);
        }
        
        /** Callback to handle Ignition state updates
        * @param int Current Ignition State
        */
        @Override
        public void onUpdateIgnitionSate(final int state) {
            boolean carIsOn = (state > 0) ? true : false; 
            if(mMediaPlayerConnected && mRadioMode == RadioModes.AUX){
                if(carIsOn){
                    if(!mIsPlaying && mWasPlaying){
                        // Post a runnable to play the last song in 5 seconds
                        mHandler.postDelayed(new Runnable(){
                            @Override
                            public void run(){
                                mPlayerService.play();
                                mIsPlaying = true;
                            }
                        }, 5000);
                        mWasPlaying = false;
                    }
                    // This if will only trigger if the car WAS previously off
                    if(mIgnitionState == IgnitionStates.OFF){
                        // Wait 5 seconds to send info request to make sure the 
                        // BM53 is on the right screen, getting RDS updates
                        mHandler.postDelayed(mSendInfoButton, 5000);
                    }
                }else{
                    if(mIsPlaying){
                        mPlayerService.pause();
                        mIsPlaying = false;
                        mWasPlaying = true;
                    }
                }
            }
            mIgnitionState = (carIsOn) ? IgnitionStates.ON : IgnitionStates.OFF;
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
            if(mMFLPlaybackMode == MFLPlaybackModes.PRESS){
                togglePlaybackInAux();
            }
        }
        
        @Override
        public void onVoiceBtnHold(){
            // Re-purpose this button to pause/play music
            if(mMFLPlaybackMode == MFLPlaybackModes.HOLD){
                togglePlaybackInAux();
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
            if(mCDPlayerPlaying){
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
        public void onLightStatus(int lightStatus){
            if(!mSettings.getBoolean("nightColorsWithInterior", false)){
                return;
            }
            int color = (lightStatus == 1) ? R.color.nightColor : R.color.dayColor;
            // Only change the color if it's different
            if(color != mCurrentTextColor){
                mCurrentTextColor = color;
                changeTextColors(mRadioLayout, color);
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
    
    private void changeRadioMode(final RadioModes desiredMode){
        if(mRadioType == RadioTypes.BM53 && getIBusLinkState()){
            mRadioModeSatisfied = false;
            mThreadExecutor.execute(new Runnable(){
                
                @Override
                public void run(){
                    while(desiredMode != mRadioMode && !mRadioModeSatisfied){
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
                            Log.e(TAG, CTAG + "mModeChanger InterruptedException");
                        }
                        if(desiredMode == mRadioMode){
                            mRadioModeSatisfied = true;
                        }
                    }
                    mRadioModeSatisfied = true;
                    return;
                }

            });
        }
    }
    
    /**
     * Reset the elements of the music player view
     * to their default values
     */
    private void clearMusicPlayerView(){
        mPlayerTitleText.setText(R.string.defaultText);
        mPlayerAlbumText.setText(R.string.defaultText);
        mPlayerArtistText.setText(R.string.defaultText);
        mPlayerArtwork.setImageResource(0);
    }
    
    private String getAppFromPackageName(String cname){
        String appName = "";
        try {
            appName = (String) mPackageManager.getApplicationLabel(
                mPackageManager.getApplicationInfo(cname, 0)
            );
        } catch (NameNotFoundException e) {
            Log.e(TAG, CTAG + "Package Name not found for package " + cname);
            appName = cname;
        }
        return appName;
    }
    
    public void sendPressReleaseCommand(String cmdName){
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
            showToast("Error sending unknown IBus Command " + cmdName);
        }
    }
    
    /**
     * Update our spinner's list of remotes based on input String array
     * @param remoteList String array of the currently available remotes
     */
    private void setMediaControllerSelection(String[] remoteList){
        if(mMediaControllerSelectorAdapter == null){
            mMediaControllerSelectorAdapter = new ArrayAdapter<String>(
                getActivity().getApplicationContext(),
                android.R.layout.simple_spinner_item,
                mMediaControllerSelectorList
            );
            mMediaControllerSelectorAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            );
            mMediaSessionSelector.setAdapter(mMediaControllerSelectorAdapter);
        }else{
            List<String> availRemotes = Arrays.asList(remoteList);
            // Add new items that don't already exist
            for(String remote: availRemotes){
                String cannonicalName = getAppFromPackageName(remote);
                mMediaControllerNames.put(cannonicalName, remote);
                if(!mMediaControllerSelectorList.contains(cannonicalName)){
                    mMediaControllerSelectorList.add(cannonicalName);
                }
            }
            // Remove items that no longer exist
            for(int i = 0; i < mMediaControllerSelectorList.size(); i++){
                String remote = mMediaControllerSelectorList.get(i);
                String className = mMediaControllerNames.get(remote);
                if(!availRemotes.contains(className)){
                    mMediaControllerSelectorList.remove(remote);
                }
            }
            // Make sure the active player is selected
            MediaController amc = mPlayerService.getMediaController();
            if(amc != null){
                mMediaSessionSelector.setSelection(
                    mMediaControllerSelectorList.indexOf(
                        getAppFromPackageName(amc.getPackageName())
                    )
                );
            }
            mMediaControllerSelectorAdapter.notifyDataSetChanged();
        }
    }
    
    private void setMediaMetadata(MediaMetadata md){
        if(md != null){
            mSongDuration = md.getLong(
                MediaMetadata.METADATA_KEY_DURATION
            );
            mPlayerTitleText.setText(
                md.getString(MediaMetadata.METADATA_KEY_TITLE)
            );
            mPlayerAlbumText.setText(
                md.getString(MediaMetadata.METADATA_KEY_ALBUM)
            );
            mPlayerArtistText.setText(
                md.getString(MediaMetadata.METADATA_KEY_ARTIST)
            );
            Bitmap albumArt = md.getBitmap(
                MediaMetadata.METADATA_KEY_ALBUM_ART
            );
            if(albumArt != null){
                mPlayerArtwork.setImageBitmap(
                    Bitmap.createScaledBitmap(
                        albumArt, ARTWORK_HEIGHT, ARTWORK_WIDTH, false
                    )
                );
            }else{
                mPlayerArtwork.setImageResource(0);
            }
            mPlayerScrubBar.setMax((int) mSongDuration);
        }else{
            Log.e(TAG, CTAG + "MediaMetadata is null");
        }
    }
    
    private void setPlaybackState(int playbackState){
        switch(playbackState){
            case PlaybackState.STATE_PLAYING:
                mIsPlaying = true;
                mPlayerControlBtn.setImageResource(
                    android.R.drawable.ic_media_pause
                );
                break;
            default:
                mIsPlaying = false;
                mPlayerControlBtn.setImageResource(
                    android.R.drawable.ic_media_play
                );
                break;
        }
    }
    
    private void setPlaybackState(PlaybackState playbackState){
        if(playbackState != null){
            setPlaybackState(playbackState.getState());
        }else{
            Log.e(TAG, CTAG + "PlaybackState is null");
        }
    }
    
    private void togglePlayback(){
        if(mIsPlaying){
            Log.d(TAG, CTAG + "mIsPlaying");
            mPlayerService.pause();
            mIsPlaying = false;
            setPlaybackState(PlaybackState.STATE_PAUSED);
        }else{
            Log.d(TAG, CTAG + "!mIsPlaying");
            mIsPlaying = true;
            mPlayerService.play();
            setPlaybackState(PlaybackState.STATE_PLAYING);
        }
    }
    
    private void togglePlaybackInAux(){
        if(mRadioMode == RadioModes.AUX){
            togglePlayback();
        }
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
	CTAG = "DashboardMusicFragment: ";
        Log.d(TAG, CTAG + "onActivityCreated Called");
        mPackageManager = getActivity().getApplicationContext(
	).getPackageManager();
        if(!mIBusConnected){
            serviceStarter(IBusMessageService.class, mIBusConnection);
        }
        if(!mMediaPlayerConnected){
            serviceStarter(MusicControllerService.class, mPlayerConnection);
        }
    }
    
    @Override
    public View onCreateView(
	LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ){
        final View v = inflater.inflate(
	    R.layout.dashboard_music, container, false
	);
        Log.d(TAG, CTAG + "onCreateView()");
        
        // Load Activity Settings
        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        // Radio Type
        mRadioType = RadioTypes.valueOf(mSettings.getString("radioType", "BM53"));
        
        // MFL Playback Button
        mMFLPlaybackMode = MFLPlaybackModes.valueOf(
            mSettings.getString("mflMediaButton", "PRESS")
        );
        
        // Layouts
        mRadioLayout = (LinearLayout) v.findViewById(R.id.radioAudio);
        mTabletLayout = (LinearLayout) v.findViewById(R.id.tabletAudio);
        mMetaDataLayout = (LinearLayout) v.findViewById(R.id.playerMetaDataLayout);

        // Music Player
        mPlayerPrevBtn = (ImageButton) v.findViewById(R.id.playerPrevBtn);
        mPlayerControlBtn = (ImageButton) v.findViewById(R.id.playerPlayPauseBtn);
        mPlayerNextBtn = (ImageButton) v.findViewById(R.id.playerNextBtn);

        mPlayerTitleText = (TextView) v.findViewById(R.id.playerTitleField);
        mPlayerAlbumText = (TextView) v.findViewById(R.id.playerAlbumField);
        mPlayerArtistText = (TextView) v.findViewById(R.id.playerArtistField);

        mPlayerArtwork = (ImageView) v.findViewById(R.id.albumArt);

        mPlayerScrubBar = (SeekBar) v.findViewById(R.id.playerTrackBar);
        
        mMediaSessionSelector = (Spinner) v.findViewById(R.id.mediaSessionSelector);
        
        // Radio Fields
        mStationText = (TextView) v.findViewById(R.id.stationText);
        mRDSField = (TextView) v.findViewById(R.id.radioRDSIndicator);
        mStereoField = (TextView) v.findViewById(R.id.radioStereoIndicator);
        mProgramField = (TextView) v.findViewById(R.id.radioProgram);
        mBroadcastField = (TextView) v.findViewById(R.id.radioBroadcast);
        
        // Radio Controls
        mBtnMusicMode = (Switch) v.findViewById(R.id.btnMusicMode);
        
        ImageButton btnVolUp = (ImageButton) v.findViewById(R.id.btnVolUp);
        ImageButton btnVolDown = (ImageButton) v.findViewById(R.id.btnVolDown);
        Button btnRadioFM = (Button) v.findViewById(R.id.btnRadioFM);
        Button btnRadioAM = (Button) v.findViewById(R.id.btnRadioAM);
        ImageButton btnPrev = (ImageButton) v.findViewById(R.id.btnPrev);
        ImageButton btnNext = (ImageButton) v.findViewById(R.id.btnNext);
        
        // Register button commands
        if(mRadioType == RadioTypes.BM53){
            btnVolUp.setTag(IBusCommand.Commands.BMToRadioVolumeUp);
            btnVolDown.setTag(IBusCommand.Commands.BMToRadioVolumeDown);
            btnPrev.setTag("BMToRadioTuneRev");
            btnNext.setTag("BMToRadioTuneFwd");
        }else{
            btnVolUp.setTag(IBusCommand.Commands.SWToRadioVolumeUp);
            btnVolDown.setTag(IBusCommand.Commands.SWToRadioVolumeDown);
            btnPrev.setTag("SWToRadioTuneRev");
            btnNext.setTag("SWToRadioTuneFwd");
        }
        
        btnRadioFM.setTag("BMToRadioFM");
        btnRadioAM.setTag("BMToRadioAM");
        
        // Scroll Title
        mPlayerTitleText.setSelected(true);
        
        // Set Adapter for music player selector
        mMediaControllerSelectorAdapter = new ArrayAdapter<String>(
            getActivity().getApplicationContext(),
            android.R.layout.simple_spinner_item,
            mMediaControllerSelectorList
        );
        mMediaControllerSelectorAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        mMediaSessionSelector.setAdapter(mMediaControllerSelectorAdapter);
        
        // Set listeners onto objects
        btnVolUp.setOnClickListener(mCommandClickListener);
        btnVolDown.setOnClickListener(mCommandClickListener);
        btnRadioFM.setOnTouchListener(mCommandTouchListener);
        btnRadioAM.setOnTouchListener(mCommandTouchListener);
        btnPrev.setOnTouchListener(mCommandTouchListener);
        btnNext.setOnTouchListener(mCommandTouchListener);
        
        mPlayerPrevBtn.setOnClickListener(mPlaybackClickListener);
        mPlayerNextBtn.setOnClickListener(mPlaybackClickListener);
        mPlayerControlBtn.setOnClickListener(mPlaybackClickListener);
        mBtnMusicMode.setOnCheckedChangeListener(mModeChangeListener);
        
        mMetaDataLayout.setOnTouchListener(mMetadataTouchListener);
        mMediaSessionSelector.setOnItemSelectedListener(mMediaSessionSelectedListener);
        mPlayerScrubBar.setOnSeekBarChangeListener(mSeekbarChangeListener);

        // Hide the toggle slider for CD53 units
	if(mRadioType != RadioTypes.BM53){
	    mBtnMusicMode.setVisibility(View.GONE);
	    mRadioLayout.setVisibility(View.GONE);
	    mTabletLayout.setVisibility(View.VISIBLE);
	}
        return v;
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, CTAG + "onResume()");
        if(mMediaPlayerConnected){
            if(mPlayerService.getMediaController() == null){
                clearMusicPlayerView();
            }
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
            mPlayerService.unregisterCallback(mPlayerCallbacks);
            serviceStopper(MusicControllerService.class, mPlayerConnection);
        }
    }
}
