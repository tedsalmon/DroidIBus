package net.littlebigisland.droidibus.activity;
/**
 * Control Fragment for IBus UI 
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus.activity
 */
import java.util.Calendar;
import net.littlebigisland.droidibus.R;
import net.littlebigisland.droidibus.ibus.IBusCommand;
import net.littlebigisland.droidibus.ibus.IBusCommandsEnum;
import net.littlebigisland.droidibus.ibus.IBusCallbackReceiver;
import net.littlebigisland.droidibus.ibus.IBusMessageService;
import net.littlebigisland.droidibus.ibus.IBusMessageService.IOIOBinder;
import net.littlebigisland.droidibus.music.MusicControllerService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteController;
import android.media.RemoteController.MetadataEditor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class DashboardFragment extends Fragment {
	public String TAG = "DroidIBus";
	
	protected Handler mHandler = new Handler();
	
	protected SharedPreferences mSettings = null;
	
	protected IBusMessageService mIBusService;
	protected boolean mIBusBound = false;
	
	// Fields in the activity
	protected TextView stationText, radioRDSIndicatorField, radioProgramField,
					   radioBroadcastField, radioStereoIndicatorField, 
					   speedField, rpmField, rangeField, outTempField,
					   coolantTempField, fuel1Field, fuel2Field, avgSpeedField,
					   geoCoordinatesField ,geoStreetField, geoLocaleField,
					   geoAltitudeField, dateField, timeField, ikeDisplayField;
	
	private TextView speedUnit, avgSpeedUnit, rangeFieldUnit, consumption1Unit,
		consumption2Unit, outdoorTempUnit, coolantTempUnit;
	
	// Views in the Activity
	protected RelativeLayout dashboardLayout;
	protected LinearLayout radioLayout, tabletLayout;
	protected ImageButton mPlayerPrevBtn, mPlayerControlBtn, mPlayerNextBtn;
	protected TextView mPlayerArtistText, mPlayerTitleText, mPlayerAlbumText;
	protected SeekBar mPlayerScrubBar;
	protected ImageView mPlayerArtwork;
	protected Switch mBtnMusicMode;
	
	protected MusicControllerService mPlayerService;
	protected boolean mPlayerBound = false;

	protected boolean mIsPlaying = false;
	protected boolean mWasPlaying = false; // Was the song playing before we shut
	protected long mSongDuration = 1;
	
	protected PowerManager mPowerManager = null;
	protected WakeLock screenWakeLock;
	protected boolean mScreenOn = false; // Screen on = true  Screen off = false
	
	protected RadioModes mCurrentRadioMode = null; // Current Radio Text
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
	
	private RadioTypes mRadioType = null;
	
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
					if(mScrubbingSupported) mHandler.post(mUpdateSeekBar);
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
	
	/**
	 * IBus Callback Functions
	 */ 	
	private IBusCallbackReceiver mIBusUpdateListener = new IBusCallbackReceiver() {
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
		    		if(mCurrentRadioMode == RadioModes.AUX && tabletLayout.getVisibility() == View.GONE){
		    			mBtnMusicMode.toggle();
		    		}
		    		if(!(mCurrentRadioMode == RadioModes.AUX) && tabletLayout.getVisibility() == View.VISIBLE){
		    			mBtnMusicMode.toggle();
		    		}
		    	}
		    	
		    	mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
		    	stationText.setText(text);
			}
		}
		
		@Override
		public void onUpdateRadioBrodcasts(final String broadcastType){
			mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
			radioBroadcastField.setText(broadcastType);
		}

		@Override
		public void onUpdateRadioStereoIndicator(final String stereoIndicator){
			if(radioLayout.getVisibility() == View.VISIBLE){
				mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
				int visibility = (stereoIndicator.equals("")) ? View.GONE : View.VISIBLE;
				radioStereoIndicatorField.setVisibility(visibility);
			}
		}

		@Override
		public void onUpdateRadioRDSIndicator(final String rdsIndicator){
			mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
			if(radioLayout.getVisibility() == View.VISIBLE){
				int visibility = (rdsIndicator.equals("")) ? View.GONE : View.VISIBLE;
				radioRDSIndicatorField.setVisibility(visibility);
			}
		}

		@Override
		public void onUpdateRadioProgramIndicator(final String currentProgram){
			mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
			radioProgramField.setText(currentProgram);
		}
		
		/**
		 * Callback to handle any vehicle speed Changes
		 * @param text Text to set
		 */
		@Override
		public void onUpdateSpeed(final int speed){
			if(mSettings.getString("speedUnit", "1").equals("0")){
				speedField.setText(String.valueOf(speed));
			}else{
				speedField.setText(String.valueOf((int) ((speed * 2) * 0.621371)));
			}
		}
		
		@Override
		public void onUpdateRPM(final int rpm){
			rpmField.setText(Integer.toString(rpm));
		}
		
		@Override
		public void onUpdateRange(final String range){
			rangeField.setText(range);
		}
		
		@Override
		public void onUpdateOutdoorTemp(final String temp){
			outTempField.setText(temp);
		}
		
		@Override
		public void onUpdateCoolantTemp(final int temp){
			if(mSettings.getString("temperatureUnit", "1").equals("0")){
				coolantTempField.setText(Integer.toString(temp));
			}else{
				coolantTempField.setText(
					"+" + Integer.toString(((temp * 9) / 5) + 32)
				);
			}
		}
		
		@Override
		public void onUpdateFuel1(final String mpg){
			fuel1Field.setText(mpg);
		}
		
		@Override
		public void onUpdateFuel2(final String mpg){
			fuel2Field.setText(mpg);
		}
		
		@Override
		public void onUpdateAvgSpeed(final String speed){
			avgSpeedField.setText(speed);
		}
		
		@Override
		public void onUpdateTime(final String time){
			timeField.setText(time);
		}
		
		@Override
		public void onUpdateDate(final String date){
			dateField.setText(date);
		}

		@Override
		public void onUpdateIgnitionSate(final int state) {
	    	boolean carState = (state > 0) ? true : false;
	    	if(carState){
	    		// The screen isn't on but the car is, turn it on
	    		if(!mScreenOn){
	    			changeScreenState(true);
	    			if(mPlayerBound && mCurrentRadioMode == RadioModes.AUX && !mIsPlaying && mWasPlaying){
	    				// Post a runnable to play the last song in 3.5 seconds
	    				new Handler(getActivity().getMainLooper()).postDelayed(new Runnable(){
							@Override
							public void run() {
								mIsPlaying = true;
								mPlayerService.sendPlayKey();
							}
						}, 3500);
	    				mWasPlaying = false;
	    			}
    			}
	    	}else{
	    		// The car is not on and the screen is, turn it off
	    		if(mScreenOn){
	    			// Pause the music as we exit the vehicle
	    			if(mPlayerBound && mCurrentRadioMode == RadioModes.AUX && mIsPlaying){
	    				mPlayerService.sendPauseKey();
	    				mIsPlaying = false;
	    				mWasPlaying = true;
	    			}
	    			changeScreenState(false);
	    			// Blank out values that aren't set while the car isn't on
	    			speedField.setText(R.string.defaultText);
	    			rpmField.setText(R.string.defaultText);
	    			coolantTempField.setText(R.string.defaultText);
	    		}
	    	}
		}

		@Override
		public void onUpdateStreetLocation(final String streetName) {
			geoStreetField.setText(streetName);
		}
		
		@Override
		public void onUpdateLocale(final String cityName) {
			geoLocaleField.setText(cityName);
		}

		@Override
		public void onUpdateGPSCoordinates(final String gpsCoordinates){
			geoCoordinatesField.setText(gpsCoordinates);
		}
		
		@Override
		public void onUpdateGPSAltitude(final int altitude){
			geoAltitudeField.setText(String.format("%s'", (int) Math.round(altitude * 3.28084)));
		}

		@Override
		public void onTrackFwd(){
			if(mPlayerBound){
				mPlayerService.sendNextKey();
			}
		}

		@Override
		public void onTrackPrev(){
			if(mPlayerBound){
				mPlayerService.sendPreviousKey();
			}
		}
		
		@Override
		public void onVoiceBtnPress(){
			// Re-purpose this button to pause/play music
	    	if(mPlayerBound && mCurrentRadioMode == RadioModes.AUX){
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
					changeTextColors(dashboardLayout, color);
				}
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
			updateDisplayedUnits();
		}
		
		@Override
		public void onUpdateIKEDisplay(String text){
			ikeDisplayField.setText(text);
		}
		
	};
	
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
    		mPlayerBound = true;
    	}

		@Override
		public void onServiceDisconnected(ComponentName name) {
        	Log.e(TAG, "MusicPlayerService is disconnected");
        	mPlayerBound = false;
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
					showToast("Unable to start; Cannot bind ourselves to the IBus Service");
				}
				
				// Emulate the BM
				BMBootup();
				
				/* This thread should make sure to send out and request
				 * any IBus messages that the BM usually would.
				 * We should also make sure to keep the radio in "Info"
				 * mode at all times here.
				 *  *** This is only required if the user doesn't have a CD53 *** 
				 */
				if(mRadioType == RadioTypes.BM53){
					new Thread(new Runnable() {
						public void run() {
							mLastRadioStatus = 0;
							while(mIBusBound){
								try{
									if(mIBusService.getLinkState()){
		    							getActivity().runOnUiThread(new Runnable(){
		    								@Override
		    								public void run(){
		    									long currentTime = Calendar.getInstance().getTimeInMillis();
		    									// BM Emulation
		    									
		    									// Ask the radio for it's status
		    									if((currentTime - mLastRadioStatusRequest) >= 10000){
		    										sendIBusCommand(IBusCommandsEnum.BMToRadioGetStatus);
		    										mLastRadioStatusRequest = currentTime;
		    									}
		    									
		    									long statusDiff = currentTime - mLastRadioStatus;
		    									if(statusDiff > 10000 && ! (mCurrentRadioMode == RadioModes.AUX)){
													sendIBusCommand(IBusCommandsEnum.BMToRadioInfoPress);
													sendIBusCommandDelayed(IBusCommandsEnum.BMToRadioInfoRelease, 500);
		    									}
		    								}
		    							});
		    							Thread.sleep(5000);
									}else{
										Thread.sleep(500); // More aggressive since the IOIO could connect at any time
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
            mIBusBound = false;
        }
    };
    
	private Runnable mUpdateSeekBar = new Runnable() {
		@Override
		public void run() {
			if(mPlayerBound) {
				mPlayerScrubBar.setProgress(
					(int) (mPlayerService.getEstimatedPosition() * mPlayerScrubBar.getMax() / mSongDuration)
				);
				mHandler.postDelayed(this, 100);
			}
		}
	};
	
	private void BMBootup(){
		// Emulate BM Boot Up
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
		
		Intent playerIntent = new Intent(applicationContext, MusicControllerService.class);
		try{
			Log.d(TAG, "Starting Music Player service");
			applicationContext.bindService(playerIntent, mPlayerConnection, Context.BIND_AUTO_CREATE);
		}
		catch(Exception ex){
			Log.e(TAG, "Unable to Start Music Player service!");
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
		
		if(mPlayerBound){
			mPlayerService.disableController();
			try{
				Log.d(TAG, "Unbinding from Music Player service");
				applicationContext.unbindService(mPlayerConnection);
				mPlayerBound = false;
			}
			catch(Exception ex){
				Log.e(TAG, "Unable to unbind the Music Player service!");
			}
		}
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG, "Dashboard: onCreate Called");
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.dashboard, container, false);
		Log.d(TAG, "Dashboard: onCreateView Called");
		
		// Load Activity Settings
		mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		
		speedUnit = (TextView) v.findViewById(R.id.speedFieldUnit);
		avgSpeedUnit = (TextView) v.findViewById(R.id.avgSpeedUnit);
		rangeFieldUnit = (TextView) v.findViewById(R.id.rangeFieldUnit);
		consumption1Unit = (TextView) v.findViewById(R.id.consumption1Unit);
		consumption2Unit = (TextView) v.findViewById(R.id.consumption2Unit);
		outdoorTempUnit = (TextView) v.findViewById(R.id.outdoorTempUnit);
		coolantTempUnit = (TextView) v.findViewById(R.id.coolantTempUnit);

		// Set the settings
		updateDisplayedUnits();
		
		// Radio Type
		String radioType = mSettings.getString("radioType", "BM53");
		mRadioType = (radioType.equals("BM53")) ? RadioTypes.BM53 : RadioTypes.CD53;
		
		// Keep a wake lock
    	changeScreenState(true);
    	
		// Layouts
    	dashboardLayout = (RelativeLayout) v.findViewById(R.id.dashboardLayout);
    	radioLayout = (LinearLayout) v.findViewById(R.id.radioAudio);
    	tabletLayout = (LinearLayout) v.findViewById(R.id.tabletAudio);

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
				if(mPlayerBound){
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
				if(mPlayerBound && fromUser) {
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
		stationText = (TextView) v.findViewById(R.id.stationText);
		radioRDSIndicatorField = (TextView) v.findViewById(R.id.radioRDSIndicator);
		radioStereoIndicatorField = (TextView) v.findViewById(R.id.radioStereoIndicator);
		radioProgramField = (TextView) v.findViewById(R.id.radioProgram);
		radioBroadcastField = (TextView) v.findViewById(R.id.radioBroadcast);

		
		// OBC Fields
		speedField = (TextView) v.findViewById(R.id.speedField);
		rpmField = (TextView) v.findViewById(R.id.rpmField);
		rangeField = (TextView) v.findViewById(R.id.rangeField);
		fuel1Field = (TextView) v.findViewById(R.id.consumption1);
		fuel2Field = (TextView) v.findViewById(R.id.consumption2);
		avgSpeedField = (TextView) v.findViewById(R.id.avgSpeed);
		
		// Temperature
		outTempField = (TextView) v.findViewById(R.id.outdoorTempField);
		coolantTempField = (TextView) v.findViewById(R.id.coolantTempField);
		
		// Geo Fields
		geoCoordinatesField = (TextView) v.findViewById(R.id.geoCoordinatesField);
		geoStreetField = (TextView) v.findViewById(R.id.geoStreetField);
		geoLocaleField = (TextView) v.findViewById(R.id.geoLocaleField);
		geoAltitudeField = (TextView) v.findViewById(R.id.geoAltitudeField);
		
		// Time & Date Fields
		dateField = (TextView) v.findViewById(R.id.dateField);
		timeField = (TextView) v.findViewById(R.id.timeField);
		
		// IKE Display Field [Current for StealthOne Support]
		ikeDisplayField = (TextView) v.findViewById(R.id.ikeDisplayField);
		
		boolean stealthOneAvailable = mSettings.getBoolean("stealthOneAvailable", false);
		if(stealthOneAvailable == false){
			ikeDisplayField.setVisibility(View.GONE);
		}

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
		    		radioLayout.setVisibility(View.GONE);
		    		tabletLayout.setVisibility(View.VISIBLE);
		        }else{
		        	if(mIsPlaying){
		        		mPlayerService.sendPauseKey();
		        	}
		    		// Send IBus Message
		    		if((mCurrentRadioMode == RadioModes.AUX || mCurrentRadioMode == RadioModes.CD) && mRadioType == RadioTypes.BM53){
		    			changeRadioMode(RadioModes.Radio);
		    		}
		    		radioLayout.setVisibility(View.VISIBLE);
		    		tabletLayout.setVisibility(View.GONE);
		        }
		    }
		});
		
		// Hide the toggle slider for CD53 units
		if(mRadioType == RadioTypes.CD53){
			mCurrentRadioMode = RadioModes.AUX;
			mBtnMusicMode.setVisibility(View.GONE);
    		radioLayout.setVisibility(View.GONE);
    		tabletLayout.setVisibility(View.VISIBLE);
		}

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
		
		fuel1Field.setTag(IBusCommandsEnum.BMToIKEResetFuel1.name());
		fuel2Field.setTag(IBusCommandsEnum.BMToIKEResetFuel1.name());
		avgSpeedField.setTag(IBusCommandsEnum.BMToIKEResetAvgSpeed.name());

		fuel1Field.setOnLongClickListener(valueResetter);
		fuel2Field.setOnLongClickListener(valueResetter);
		avgSpeedField.setOnLongClickListener(valueResetter);
		
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
		
		//Act on preferences
		boolean geoAvailable = mSettings.getBoolean("navAvailable", false);
		if(!geoAvailable){
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
		
		return v;
	}
	
	@Override
	public void onActivityCreated (Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, "Dashboard: onActivityCreated Called");
		// Bind required background services last since the callback
		// functions depend on the view items being initialized
		if(!mIBusBound){
			bindServices();
		}
	}
	
	private void changeRadioMode(final RadioModes mode){
		new Thread(new Runnable(){
			public void run(){
				try{
					if(mRadioType == RadioTypes.BM53){
						Log.d(TAG, String.format("Current mode = %s, desired mode = %s", mCurrentRadioMode.toString(), mode.toString() ));
						if((mode == RadioModes.AUX && !(mCurrentRadioMode== RadioModes.AUX)) ||  
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
	
	public void updateDisplayedUnits(){
		// Set the units
		
		// Consumption
		switch(mSettings.getString("consumptionUnit", "10")){
			case "00":
				consumption1Unit.setText("L/100");
				consumption2Unit.setText("L/100");
				break;
			case "01":
				consumption1Unit.setText("MPG");
				consumption2Unit.setText("MPG");
				break;
			case "11":
				consumption1Unit.setText("KM/L");
				consumption2Unit.setText("KM/L");
				break;
		}
		
		// Speed - Avg Speed and Current Speed
		if(mSettings.getString("speedUnit", "1").equals("0")){
			speedUnit.setText("KM/H");
			avgSpeedUnit.setText("KM/H");
		}else{
			speedUnit.setText("MPH");
			avgSpeedUnit.setText("MPH");
		}
		// Distance
		if(mSettings.getString("distanceUnit", "1").equals("0")){
			rangeFieldUnit.setText("Km");
		}else{
			rangeFieldUnit.setText("Mi");
		}
		
		// Temperature
		if(mSettings.getString("temperatureUnit", "1").equals("0")){
			outdoorTempUnit.setText("C");
			coolantTempUnit.setText("C");
		}else{
			outdoorTempUnit.setText("F");
			coolantTempUnit.setText("F");
		}
	}
	
	/**
	 * Change TextView colors recursively; Used to support night colors
	 */
    public void changeTextColors(ViewGroup view, int colorId){
		for(int i = 0; i < view.getChildCount(); i++){
			View child = view.getChildAt(i);
			 // TextView, change it's color
			if(child instanceof TextView){
				TextView c = (TextView) child;
				c.setTextColor(getResources().getColor(colorId));
			} // ViewGroup; Recurse children to find TextViews
			else if(child instanceof ViewGroup){
	            changeTextColors((ViewGroup) child, colorId);
	        }
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
			screenWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "screenWakeLock");
			window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD); 
		}
		
		if(!screenState && mScreenOn){
			modeChange = true;
			mScreenOn = false;
			layoutP.screenBrightness = 0;
			screenWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "screenWakeLock");
		}
		
		if(modeChange){
			// Release the previous wakeLock before acquiring the next one
			releaseWakelock();
			window.setAttributes(layoutP); // Set the given layout
			screenWakeLock.acquire();
		}
	}
	
	private void releaseWakelock(){
    	if(screenWakeLock != null){
    		if(screenWakeLock.isHeld()){
    			screenWakeLock.release();
    		}
    	}
	}
	
	private void showToast(String toastText){
		Context appContext = getActivity();
		Toast.makeText(appContext, toastText, Toast.LENGTH_LONG).show();
	}
	
	private void sendIBusCommand(IBusCommandsEnum cmd, Object... args){
		if(mIBusBound && mIBusService.getLinkState()){
			mIBusService.sendCommand(new IBusCommand(cmd, args));
		}
	}
	
	private void sendIBusCommandDelayed(final IBusCommandsEnum cmd, final long delayMillis, final Object... args){
		new Handler(getActivity().getMainLooper()).postDelayed(new Runnable(){
			public void run(){
				sendIBusCommand(cmd, args);
			}
		}, delayMillis);
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
		// Emulate the BM to repopulate the view
		BMBootup();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d(TAG, "Dashboard: onDestroy called");
    	mIBusService.removeCallback(mIBusUpdateListener);
    	releaseWakelock();
    	if(mIBusBound){
    		unbindServices();
    	}
    }
}