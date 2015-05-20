package net.littlebigisland.droidibus.activity;

import java.util.Calendar;

import net.littlebigisland.droidibus.R;
import net.littlebigisland.droidibus.ibus.IBusCommandsEnum;
import net.littlebigisland.droidibus.ibus.IBusCallbackReceiver;
import net.littlebigisland.droidibus.ibus.IBusMessageService;
import net.littlebigisland.droidibus.ibus.IBusMessageService.IOIOBinder;
import net.littlebigisland.droidibus.music.MusicControllerService;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteController;
import android.media.RemoteController.MetadataEditor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

@SuppressWarnings("deprecation")
public class DashboardMusicFragment extends BaseFragment{

	protected Handler mHandler = new Handler();
    protected SharedPreferences mSettings = null;
    
    // Fields in the activity
    protected TextView mStationText, mRDSField, mProgramField,
        mBroadcastField, mStereoField;

    // Views in the Activity
    protected LinearLayout mRadioLayout, mTabletLayout;
    protected ImageButton mPlayerPrevBtn, mPlayerControlBtn, mPlayerNextBtn;
    protected TextView mPlayerArtistText, mPlayerTitleText, mPlayerAlbumText;
    protected SeekBar mPlayerScrubBar;
    protected ImageView mPlayerArtwork;
    protected Switch mBtnMusicMode;
    
    protected MusicControllerService mPlayerService;
    protected boolean mMediaPlayerConnected = false;

    protected boolean mIsPlaying = false;
    protected boolean mWasPlaying = false; // Was the song playing before we shut
    protected long mSongDuration = 1;

    protected RadioModes mCurrentRadioMode = null; // Current Radio Text
    protected RadioTypes mRadioType = null; // Users radio type
    protected long mLastRadioStatus = 0; // Epoch of last time we got a status message from the Radio
    protected long mLastModeChange = 0; // Time that the radio mode last changed
    protected long mLastRadioStatusRequest = 0; // Time we last requested the Radio's status
    protected boolean mCDPlayerPlaying = false;

	private enum RadioModes{
		AUX,
		CD,
		Radio
	}
	
	private enum RadioTypes{
		BM53,
		CD53
	}

    private ServiceConnection mPlayerConnection = new ServiceConnection(){
        
        @Override
        public void onServiceConnected(ComponentName className, IBinder service){
            // Getting the binder and activating RemoteController instantly
            MusicControllerService.PlayerBinder binder = (MusicControllerService.PlayerBinder) service;
            mPlayerService = binder.getService();
            try{
                mPlayerService.enableController();
            }catch(RuntimeException ex){
                showToast("Please enable Notification access for DroidIBus in Settings > Security > Notification Access then restart the application!");
            }
            mPlayerService.setCallbackListener(mPlayerUpdateListener);
            mMediaPlayerConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "MusicPlayerService is disconnected");
            mMediaPlayerConnected = false;
        }

    };
    
    private Runnable mUpdateSeekBar = new Runnable() {
        @Override
        public void run() {
            if(mMediaPlayerConnected) {
                mPlayerScrubBar.setProgress(
                    (int) (mPlayerService.getEstimatedPosition() * mPlayerScrubBar.getMax() / mSongDuration)
                );
                mHandler.postDelayed(this, 100);
            }
        }
    };

	private RemoteController.OnClientUpdateListener mPlayerUpdateListener = new RemoteController.OnClientUpdateListener() {

        private boolean mScrubbingSupported = false;
        
        private boolean isScrubbingSupported(int flags) {
            return (flags & RemoteControlClient.FLAG_KEY_MEDIA_POSITION_UPDATE) != 0; 
        }

        @Override
        public void onClientTransportControlUpdate(int transportControlFlags) {
            mScrubbingSupported = isScrubbingSupported(transportControlFlags);
            // If we can update the seek bar, set that up, else disable it
            if(mScrubbingSupported) {
                mPlayerScrubBar.setEnabled(true);
                mHandler.post(mUpdateSeekBar);
            }else{
                mPlayerScrubBar.setEnabled(false);
                mHandler.removeCallbacks(mUpdateSeekBar);
            }
        }

        @Override
        public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {
            stateUpdate(state);
            mPlayerScrubBar.setProgress((int) (currentPosMs * mPlayerScrubBar.getMax() / mSongDuration));
        }
        
        /**
         * Evaluate the player state and update play/pause button as needed
         */
        @Override
        public void onClientPlaybackStateUpdate(int state) {
            stateUpdate(state);
        }
        
        private void stateUpdate(int state){
            switch(state){
                case RemoteControlClient.PLAYSTATE_PLAYING:
                    if(mScrubbingSupported){
                    	mHandler.post(mUpdateSeekBar);
                    }
                    mIsPlaying = true;
                    mPlayerControlBtn.setImageResource(android.R.drawable.ic_media_pause);
                    break;
                default:
                    mHandler.removeCallbacks(mUpdateSeekBar);
                    mIsPlaying = false;
                    mPlayerControlBtn.setImageResource(android.R.drawable.ic_media_play);
                    break;
            }
        }
        
        @Override
        public void onClientMetadataUpdate(MetadataEditor editor) {
            // Some players write artist name to METADATA_KEY_ALBUMARTIST instead of METADATA_KEY_ARTIST, so we should double-check it
            mPlayerArtistText.setText(editor.getString(MediaMetadataRetriever.METADATA_KEY_ARTIST, 
                editor.getString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, getString(R.string.defaultText))
            ));
            
            mPlayerTitleText.setText(editor.getString(MediaMetadataRetriever.METADATA_KEY_TITLE, getString(R.string.defaultText)));
            mPlayerAlbumText.setText(editor.getString(MediaMetadataRetriever.METADATA_KEY_ALBUM, getString(R.string.defaultText)));
            
            mSongDuration = editor.getLong(MediaMetadataRetriever.METADATA_KEY_DURATION, 1);
            mPlayerArtwork.setImageBitmap(editor.getBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, null));
        }

        @Override
        public void onClientChange(boolean clearing) {
        }
    };
    
    private IBusCallbackReceiver mIBusUpdateListener = new IBusCallbackReceiver(){
    	
    	private int mCurrentTextColor = R.color.dayColor;
		
		/**
		 * Callback to handle any updates to the station text when in Radio Mode
		 * @param text Text to set
		 */
		@Override
		public void onUpdateRadioStation(final String text){
			// If this is a BM53 unit, we should listen for
			// Updates to the station text
			if(mRadioType == RadioTypes.BM53){
				RadioModes lastState = mCurrentRadioMode; 
		    	switch(text){
		    		case "TR 01 ":
		    		case "NO CD":
		    			mCurrentRadioMode = RadioModes.CD;
		    			break;
		    		case "AUX":
		    			mCurrentRadioMode = RadioModes.AUX;
		    			break;
		    		default:
		    			mCurrentRadioMode = RadioModes.Radio;
		    			break;
		    	}
		    	
		    	if(lastState != mCurrentRadioMode){
		    		mLastModeChange = Calendar.getInstance().getTimeInMillis();
		    	}
		    	
		    	/* 
		    	 We're not in the right mode, sync with the car
		    	 Make sure this isn't CD mode and that we're not in the middle of a mode change
		    	 by making sure we've been in the current mode for at least 1.5 seconds
		    	 If lastState is null then we should also check as this is the first bit of data
		    	 see about radio mode 
		    	 */
		    	if((!(mCurrentRadioMode == RadioModes.CD) && (Calendar.getInstance().getTimeInMillis() - mLastModeChange) > 1500) || lastState == null){
		    		if(mCurrentRadioMode == RadioModes.AUX && mTabletLayout.getVisibility() == View.GONE){
		    			mBtnMusicMode.toggle();
		    		}
		    		if(!(mCurrentRadioMode == RadioModes.AUX) && mTabletLayout.getVisibility() == View.VISIBLE){
		    			mBtnMusicMode.toggle();
		    		}
		    	}
		    	
		    	mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
		    	mStationText.setText(text);
			}
		}
		
    	@Override
        public void onUpdateRadioBrodcasts(final String broadcastType){
            mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
            mBroadcastField.setText(broadcastType);
        }

        @Override
        public void onUpdateRadioStereoIndicator(final String stereoIndicator){
            if(mRadioLayout.getVisibility() == View.VISIBLE){
                mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
                int visibility = (stereoIndicator.equals("")) ? View.GONE : View.VISIBLE;
                mStereoField.setVisibility(visibility);
            }
        }

        @Override
        public void onUpdateRadioRDSIndicator(final String rdsIndicator){
            mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
            if(mRadioLayout.getVisibility() == View.VISIBLE){
                int visibility = (rdsIndicator.equals("")) ? View.GONE : View.VISIBLE;
                mRDSField.setVisibility(visibility);
            }
        }

        @Override
        public void onUpdateRadioProgramIndicator(final String currentProgram){
            mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
            mProgramField.setText(currentProgram);
        }
        
	    /** Callback to handle Ignition state updates
	     * @param int Current Ignition State
	     */
	    @Override
		public void onUpdateIgnitionSate(final int state) {
	    	boolean carState = (state > 0) ? true : false;
	    	if(carState){
	            if(mMediaPlayerConnected && mCurrentRadioMode == RadioModes.AUX && !mIsPlaying && mWasPlaying){
	                // Post a runnable to play the last song in 3.5 seconds
	                new Handler(getActivity().getMainLooper()).postDelayed(new Runnable(){
	                    @Override
	                    public void run() {
	                        mIsPlaying = true;
	                        mPlayerService.sendPlayKey();
	                    }
	                }, 1000);
	                mWasPlaying = false;
	            }
	    	}else{
	            if(mMediaPlayerConnected && mCurrentRadioMode == RadioModes.AUX && mIsPlaying){
	                mPlayerService.sendPauseKey();
	                mIsPlaying = false;
	                mWasPlaying = true;
	            }
	    	}
	    }
	    
		@Override
		public void onTrackFwd(){
			if(mMediaPlayerConnected){
				mPlayerService.sendNextKey();
			}
		}

		@Override
		public void onTrackPrev(){
			if(mMediaPlayerConnected){
				mPlayerService.sendPreviousKey();
			}
		}
		
		@Override
		public void onVoiceBtnPress(){
			// Re-purpose this button to pause/play music
	    	if(mMediaPlayerConnected && mCurrentRadioMode == RadioModes.AUX){
	    		if(mIsPlaying){
					mPlayerService.sendPauseKey();
					mIsPlaying = true;
	    		}else{
					mPlayerService.sendPlayKey();
					mIsPlaying = false;
	    		}
	    	}
		}

		@Override
		public void onUpdateRadioStatus(int status){
			// Radio is off, turn it on
			if(status == 0){
				sendIBusCommand(IBusCommandsEnum.BMToRadioPwrPress);
				sendIBusCommandDelayed(IBusCommandsEnum.BMToRadioPwrRelease, 500);
			}
		}

		@Override
		public void onRadioCDStatusRequest(){
			// Tell the Radio we have a CD on track 1
			byte trackAndCD = (byte) 0x01;
	    	sendIBusCommand(IBusCommandsEnum.BMToRadioCDStatus, 0, trackAndCD, trackAndCD);
			if(!mCDPlayerPlaying){
				sendIBusCommand(IBusCommandsEnum.BMToRadioCDStatus, 0, trackAndCD, trackAndCD);
			}else{
				sendIBusCommand(IBusCommandsEnum.BMToRadioCDStatus, 1, trackAndCD, trackAndCD);
			}
		}
	    
		@Override
		public void onLightStatus(int lightStatus){
			if(mSettings.getBoolean("nightColorsWithInterior", false)){
				int color = (lightStatus == 1) ? R.color.nightColor : R.color.dayColor;
				// Only change the color if it's different
				if(color != mCurrentTextColor){
					mCurrentTextColor = color;
					changeTextColors(mRadioLayout, color);
				}
			}
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
                	showToast("Unable to start; Cannot bind handler to the IBus Service");
                }
                Log.d(TAG, "DashboardMusic: IBus Service Bound");
            	/* This thread should make sure to send out and request
    	         * any IBus messages that the BM usually would.
    	         * We should also make sure to keep the radio in "Info"
    	         * mode at all times here.
    	         *  -This is only required if the user has a BM53-
    	         */
		    	if(mRadioType == RadioTypes.BM53){
		    	    new Thread(new Runnable() {
		    	        public void run() {
		    	        	final int radioStatusTimeout = 5000;
		    	            mLastRadioStatus = 0;
		    	            while(mIBusConnected){
		    	                try{
		    	                    if(mIBusService.getLinkState()){
		    	                        getActivity().runOnUiThread(new Runnable(){
		    	                            @Override
		    	                            public void run(){
		    	                                long currentTime = Calendar.getInstance().getTimeInMillis();
		    	                                // BM Emulation
		    	                                
		    	                                // Ask the radio for it's status
		    	                                if((currentTime - mLastRadioStatusRequest) >= radioStatusTimeout){
		    	                                    sendIBusCommand(IBusCommandsEnum.BMToRadioGetStatus);
		    	                                    mLastRadioStatusRequest = currentTime;
		    	                                }
		    	                                
		    	                                long statusDiff = currentTime - mLastRadioStatus;
		    	                                if(statusDiff > radioStatusTimeout && ! (mCurrentRadioMode == RadioModes.AUX)){
		    	                                    sendIBusCommand(IBusCommandsEnum.BMToRadioInfoPress);
		    	                                    sendIBusCommandDelayed(IBusCommandsEnum.BMToRadioInfoRelease, 500);
		    	                                }
		    	                            }
		    	                        });
		    	                        Thread.sleep(5000);
		    	                    }else{
		    	                    	// Aggressive timing since the IOIO could connect at any time
		    	                        Thread.sleep(500);
		    	                    }
		    	                }catch(InterruptedException e){
		    	                    // First world anarchy
		    	                }
		    	            }
		    	        }
		    	    }).start();
		    	}
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "mIBusService is disconnected");
            mIBusConnected = false;
        }

    };
    
    private void changeRadioMode(final RadioModes mode){
        new Thread(new Runnable(){
            public void run(){
                try{
                	if(mRadioType == RadioTypes.BM53){
	                    Log.d(TAG, String.format("Current mode = %s, desired mode = %s", mCurrentRadioMode.toString(), mode.toString() ));
	                    if( (mode == RadioModes.AUX && !(mCurrentRadioMode== RadioModes.AUX)) ||  
	                        (mode == RadioModes.Radio && (mCurrentRadioMode != RadioModes.Radio)) ){
	                        sendIBusCommand(IBusCommandsEnum.BMToRadioModePress);
	                        sendIBusCommandDelayed(IBusCommandsEnum.BMToRadioModeRelease, 300);
	                        Thread.sleep(1000);
	                        changeRadioMode(mode);
	                    }
                	}
	            }catch(InterruptedException e){
	                e.printStackTrace();
	            }
            }
        }).start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "DashboardMusic: onCreate Called");
    }
    
    @Override
    public void onActivityCreated (Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "DashboardMusic: onActivityCreated Called");
        // Bind required background services last since the callback
        // functions depend on the view items being initialized
        if(!mIBusConnected){
            serviceStarter(IBusMessageService.class, mIBusConnection);
        }
        if(!mMediaPlayerConnected){
            serviceStarter(MusicControllerService.class, mPlayerConnection);
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.dashboard_music, container, false);
        Log.d(TAG, "DashboardMusic: onCreateView Called");
        
        // Load Activity Settings
        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        // Radio Type
        String radioType = mSettings.getString("radioType", "BM53");
        mRadioType = (radioType.equals("BM53")) ? RadioTypes.BM53 : RadioTypes.CD53;
        
        // Layouts
        mRadioLayout = (LinearLayout) v.findViewById(R.id.radioAudio);
        mTabletLayout = (LinearLayout) v.findViewById(R.id.tabletAudio);

        // Music Player
        mPlayerPrevBtn = (ImageButton) v.findViewById(R.id.playerPrevBtn);
        mPlayerControlBtn = (ImageButton) v.findViewById(R.id.playerPlayPauseBtn);
        mPlayerNextBtn = (ImageButton) v.findViewById(R.id.playerNextBtn);

        mPlayerTitleText = (TextView) v.findViewById(R.id.playerTitleField);
        mPlayerAlbumText = (TextView) v.findViewById(R.id.playerAlbumField);
        mPlayerArtistText = (TextView) v.findViewById(R.id.playerArtistField);

        mPlayerArtwork = (ImageView) v.findViewById(R.id.albumArt);

        mPlayerScrubBar = (SeekBar) v.findViewById(R.id.playerTrackBar);
        
        OnClickListener playerClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMediaPlayerConnected){
                    switch(v.getId()) {
                        case R.id.playerPrevBtn:
                            mPlayerService.sendPreviousKey();
                            break;
                        case R.id.playerNextBtn:
                            mPlayerService.sendNextKey();
                            break;
                        case R.id.playerPlayPauseBtn:
                            if(mIsPlaying) {
                                mIsPlaying = false;
                                mPlayerService.sendPauseKey();
                            } else {
                                mIsPlaying = true;
                                mPlayerService.sendPlayKey();
                            }
                            break;
                    }
                }
            }
        };

        mPlayerPrevBtn.setOnClickListener(playerClickListener);
        mPlayerNextBtn.setOnClickListener(playerClickListener);
        mPlayerControlBtn.setOnClickListener(playerClickListener);
        
        mPlayerScrubBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                if(mMediaPlayerConnected && fromUser) {
                    mPlayerService.seekTo(mSongDuration * progress/seekBar.getMax());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mHandler.removeCallbacks(mUpdateSeekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mHandler.post(mUpdateSeekBar);
            }
            
        });
        
        // Get the buttons from the view
        ImageButton btnVolUp = (ImageButton) v.findViewById(R.id.btnVolUp);
        ImageButton btnVolDown = (ImageButton) v.findViewById(R.id.btnVolDown);
        Button btnRadioFM = (Button) v.findViewById(R.id.btnRadioFM);
        Button btnRadioAM = (Button) v.findViewById(R.id.btnRadioAM);
        mBtnMusicMode = (Switch) v.findViewById(R.id.btnMusicMode);
        ImageButton btnPrev = (ImageButton) v.findViewById(R.id.btnPrev);
        ImageButton btnNext = (ImageButton) v.findViewById(R.id.btnNext);
        
        
        // Setup the text fields for the view
        
        // Radio Fields
        mStationText = (TextView) v.findViewById(R.id.stationText);
        mRDSField = (TextView) v.findViewById(R.id.radioRDSIndicator);
        mStereoField = (TextView) v.findViewById(R.id.radioStereoIndicator);
        mProgramField = (TextView) v.findViewById(R.id.radioProgram);
        mBroadcastField = (TextView) v.findViewById(R.id.radioBroadcast);

        // Register Button actions
        if(mRadioType == RadioTypes.BM53){
            btnVolUp.setTag(IBusCommandsEnum.BMToRadioVolumeUp.name());
            btnVolDown.setTag(IBusCommandsEnum.BMToRadioVolumeDown.name());
            btnPrev.setTag("BMToRadioTuneRev");
            btnNext.setTag("BMToRadioTuneFwd");
        }else{
            btnVolUp.setTag(IBusCommandsEnum.SWToRadioVolumeUp.name());
            btnVolDown.setTag(IBusCommandsEnum.SWToRadioVolumeDown.name());
            btnPrev.setTag("SWToRadioTuneRev");
            btnNext.setTag("SWToRadioTuneFwd");
        }
        
        btnRadioFM.setTag("BMToRadioFM");
        btnRadioAM.setTag("BMToRadioAM");

        mBtnMusicMode.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                Log.d(TAG, "Changing Music Mode");
                // Tablet Mode if checked, else Radio
                if(isChecked){
                    // Send IBus Message
                    if(! (mCurrentRadioMode == RadioModes.AUX) && mRadioType == RadioTypes.BM53){
                        changeRadioMode(RadioModes.AUX);
                    }
                    mRadioLayout.setVisibility(View.GONE);
                    mTabletLayout.setVisibility(View.VISIBLE);
                }else{
                    if(mIsPlaying){
                        mPlayerService.sendPauseKey();
                    }
                    // Send IBus Message
                    if((mCurrentRadioMode == RadioModes.AUX || mCurrentRadioMode == RadioModes.CD) && mRadioType == RadioTypes.BM53){
                        changeRadioMode(RadioModes.Radio);
                    }
                    mRadioLayout.setVisibility(View.VISIBLE);
                    mTabletLayout.setVisibility(View.GONE);
                }
            }
        });


        
        OnClickListener clickSingleAction = new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendIBusCommand(IBusCommandsEnum.valueOf(v.getTag().toString()));
            }
        };
        
        OnTouchListener touchAction = new OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                String action = (event.getAction() == MotionEvent.ACTION_DOWN) ? "Press" : "Release";
                sendIBusCommand(IBusCommandsEnum.valueOf(v.getTag().toString() + action));
                return false;
            }
        };
        
        btnVolUp.setOnClickListener(clickSingleAction);
        btnVolDown.setOnClickListener(clickSingleAction);
        btnRadioFM.setOnTouchListener(touchAction);
        btnRadioAM.setOnTouchListener(touchAction);
        btnPrev.setOnTouchListener(touchAction);
        btnNext.setOnTouchListener(touchAction);
	    
        // Hide the toggle slider for CD53 units
		if(mRadioType != RadioTypes.BM53){
			mCurrentRadioMode = RadioModes.AUX;
			mBtnMusicMode.setVisibility(View.GONE);
	 		mRadioLayout.setVisibility(View.GONE);
	 		mTabletLayout.setVisibility(View.VISIBLE);
		}
		
        return v;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "DashboardMusic: onResume called");
        if(mIBusConnected){
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

}