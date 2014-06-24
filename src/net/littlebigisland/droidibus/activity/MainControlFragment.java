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
import net.littlebigisland.droidibus.ibus.IBusMessageReceiver;
import net.littlebigisland.droidibus.ibus.IBusMessageService;
import net.littlebigisland.droidibus.ibus.IBusMessageService.IOIOBinder;
import net.littlebigisland.droidibus.music.MusicControllerService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteController;
import android.media.RemoteController.MetadataEditor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainControlFragment extends Fragment {
	public String TAG = "DroidIBus";
	
	protected IBusMessageService mIBusService;
    protected boolean mIBusBound = false;
    
    // Fields in the activity
    protected TextView stationText, radioRDSIndicatorField, radioProgramField,
    				   radioBroadcastField, radioStereoIndicatorField, 
    				   speedField, rpmField, rangeField, outTempField,
    				   coolantTempField, fuel1Field, fuel2Field, avgSpeedField,
    				   geoCoordinatesField ,geoStreetField, geoLocaleField,
    				   geoAltitudeField, dateField, timeField;

	// Views in the Activity
    protected LinearLayout radioLayout, tabletLayout;
	protected ImageButton mPlayerPrevBtn, mPlayerControlBtn, mPlayerNextBtn;
	protected TextView mPlayerArtistText, mPlayerTitleText, mPlayerAlbumText;
	protected SeekBar mPlayerScrubBar;
	protected ImageView mPlayerArtwork;
	protected Switch mBtnMusicMode;
	
	protected MusicControllerService mPlayerService;
	protected boolean mPlayerBound = false;
	
	protected Handler mPlayerHandler = new Handler();

	protected boolean mIsPlaying = false;
	protected long mSongDuration = 1;
	
	protected PowerManager mPowerManager = null;
	protected WakeLock screenWakeLock;
	
	protected RadioModes mCurrentRadioMode = null; // Current Radio Text
	protected long mLastRadioStatus = 0; // Epoch of last time we got a status message from the Radio
	protected long mTimeInCurrentMode = 0;
	
	private enum RadioModes{
		AUX,
		CD,
		Radio
	}
	
	private RemoteController.OnClientUpdateListener mPlayerUpdateListener = new RemoteController.OnClientUpdateListener() {

		private boolean mScrubbingSupported = false;
		
		private boolean isScrubbingSupported(int flags) {
			return (flags & RemoteControlClient.FLAG_KEY_MEDIA_POSITION_UPDATE) != 0; 
		}

		@Override
		public void onClientTransportControlUpdate(int transportControlFlags) {
			mScrubbingSupported = isScrubbingSupported(transportControlFlags);
			/*
			 * If we can update the seek bar, set that up, else disable it
			 */
			if(mScrubbingSupported) {
				mPlayerScrubBar.setEnabled(true);
				mPlayerHandler.post(mUpdateSeekBar);
			}else{
				mPlayerScrubBar.setEnabled(false);
				mPlayerHandler.removeCallbacks(mUpdateSeekBar);
			}
		}

		@Override
		public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {
			switch(state) {
				case RemoteControlClient.PLAYSTATE_PLAYING:
					if(mScrubbingSupported) mPlayerHandler.post(mUpdateSeekBar);
					mIsPlaying = true;
					mPlayerControlBtn.setImageResource(android.R.drawable.ic_media_pause);
					break;
				default:
					mPlayerHandler.removeCallbacks(mUpdateSeekBar);
					mIsPlaying = false;
					mPlayerControlBtn.setImageResource(android.R.drawable.ic_media_play);
					break;
			}
			
			mPlayerScrubBar.setProgress((int) (currentPosMs * mPlayerScrubBar.getMax() / mSongDuration));
		}
		
		/**
		 * Evaluate the player state and enable/disable view items as needed
		 */
		@Override
		public void onClientPlaybackStateUpdate(int state) {
			// TODO Merge with previous function, this is stupid
			switch(state){
				case RemoteControlClient.PLAYSTATE_PLAYING:
					if(mScrubbingSupported) mPlayerHandler.post(mUpdateSeekBar);
					mIsPlaying = true;
					mPlayerControlBtn.setImageResource(android.R.drawable.ic_media_pause);
					break;
				default:
					mPlayerHandler.removeCallbacks(mUpdateSeekBar);
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
	 * IBusCallback Functions
	 */
	private IBusMessageReceiver mIBusUpdateListener =  new IBusMessageReceiver() {
		
		/**
		 * Send shit back to the UI Context
		 * @param code
		 */
		private void postToUI(Runnable code){
			Context appContext = getActivity();
			Handler mainHandler = new Handler(appContext.getMainLooper());
			mainHandler.post(code);
		}
		
		/**
		 * Callback to handle any updates to the station text when in Radio Mode
		 * @param text Text to set
		 */
		@Override
		public void onUpdateRadioStation(final String text){
			Log.d(TAG, "Setting station text - '" + text + "'");
			postToUI(new Runnable() {
			    public void run() {
			    	RadioModes lastState = mCurrentRadioMode; 
			    	switch(text){
			    		case "AUX":
			    			mCurrentRadioMode = RadioModes.AUX;
			    			break;
			    		case "NO CD":
			    			mCurrentRadioMode = RadioModes.CD;
			    			break;
			    		default:
			    			mCurrentRadioMode = RadioModes.Radio;
			    	}
			    	if(lastState != mCurrentRadioMode)
			    		Log.d(TAG, "Mode change registered, resetting counter");
			    		mTimeInCurrentMode = Calendar.getInstance().getTimeInMillis();
			    	// We're not in the right mode, sync with the car
			    	// Make sure this isn't CD mode and that we're not in the middle of a mode change
			    	// by making sure we've been in the current mode for at least 1 second
			    	if(!(mCurrentRadioMode == RadioModes.CD) && mTimeInCurrentMode > 1000){
			    		if(mCurrentRadioMode == RadioModes.AUX && tabletLayout.getVisibility() == View.GONE)
			    			mBtnMusicMode.toggle();
			    		if(!(mCurrentRadioMode == RadioModes.AUX) && tabletLayout.getVisibility() == View.VISIBLE)
			    			mBtnMusicMode.toggle();
			    	}
			    	
			    	mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
			    	stationText.setText(text);
			    }
			});
		}
		
		@Override
		public void onUpdateRadioBrodcasts(final String broadcastType){
			Log.d(TAG, "Setting Radio Broadcast Type");
			postToUI(new Runnable(){
			    public void run(){
			    	mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
			    	radioBroadcastField.setText(broadcastType);
			    }
			});
		}

		@Override
		public void onUpdateRadioStereoIndicator(final String stereoIndicator){
			Log.d(TAG, "Setting Stereo Indicator - '" + stereoIndicator + "'");
			postToUI(new Runnable(){
			    public void run(){
			    	if(radioLayout.getVisibility() == View.VISIBLE){
			    		mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
			    		int visibility = (stereoIndicator.equals("")) ? View.GONE : View.VISIBLE;
			    		radioStereoIndicatorField.setVisibility(visibility);
			    	}
			    }
			});
		}

		@Override
		public void onUpdateRadioRDSIndicator(final String rdsIndicator){
			Log.d(TAG, "Setting RDS Indicator - '" + rdsIndicator + "'");
			postToUI(new Runnable(){
			    public void run(){
			    	mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
			    	if(radioLayout.getVisibility() == View.VISIBLE){
			    		int visibility = (rdsIndicator.equals("")) ? View.GONE : View.VISIBLE;
			    		radioRDSIndicatorField.setVisibility(visibility);
			    	}
			    }
			});
		}

		@Override
		public void onUpdateRadioProgramIndicator(final String currentProgram){
			Log.d(TAG, "Setting Radio Program Type");
			postToUI(new Runnable(){
			    public void run(){
			    	mLastRadioStatus = Calendar.getInstance().getTimeInMillis();
			    	radioProgramField.setText(currentProgram);
			    }
			});
		}
		
		/**
		 * Callback to handle any vehicle speed Changes
		 * @param text Text to set
		 */
		@Override
		public void onUpdateSpeed(final int speed){
			Log.d(TAG, "Setting Speed in Callback!");
			final int speedMPH = ((int) ((speed * 2) * 0.621371));
			postToUI(new Runnable() {
			    public void run() {
			    	speedField.setText(String.format("%d MPH", speedMPH));
			    }
			});
		}
		
		@Override
		public void onUpdateRPM(final int rpm){
			Log.d(TAG, "Setting RPM in Callback!");
			postToUI(new Runnable() {
			    public void run() {
			    	rpmField.setText(Integer.toString(rpm));
			    }
			});
			
		}
		
		@Override
		public void onUpdateRange(final String range){
			Log.d(TAG, "Setting Gas Range in Callback!");
			postToUI(new Runnable() {
			    public void run() {
			    	rangeField.setText(range);
			    }
			});
		}
		
		@Override
		public void onUpdateOutdoorTemp(final String temp){
			Log.d(TAG, "Setting Outdoor Temp in Callback!");
			postToUI(new Runnable() {
			    public void run() {
			    	outTempField.setText(temp);
			    }
			});
		}
		
		@Override
		public void onUpdateCoolantTemp(final int temp){
			Log.d(TAG, "Setting Coolant Temp in Callback!");
			postToUI(new Runnable() {
			    public void run() {
			    	coolantTempField.setText(Integer.toString(temp));
			    }
			});
		}
		
		@Override
		public void onUpdateFuel1(final String mpg){
			Log.d(TAG, "Setting MPG1 in Callback!");
			postToUI(new Runnable() {
			    public void run() {
			    	fuel1Field.setText(mpg);
			    }
			});
		}
		
		@Override
		public void onUpdateFuel2(final String mpg){
			Log.d(TAG, "Setting MPG2 in Callback!");
			postToUI(new Runnable() {
			    public void run() {
			    	fuel2Field.setText(mpg);
			    }
			});
		}
		
		@Override
		public void onUpdateAvgSpeed(final String speed){
			Log.d(TAG, "Setting AVG Speed in Callback!");
			postToUI(new Runnable() {
			    public void run() {
			    	avgSpeedField.setText(speed);
			    }
			});
		}
		
		@Override
		public void onUpdateTime(final String time){
			Log.d(TAG, "The time is " + time);
			postToUI(new Runnable() {
			    public void run() {
			    	timeField.setText(time);
			    }
			});
		}
		
		@Override
		public void onUpdateDate(final String date){
			Log.d(TAG, "The date is " + date);
			postToUI(new Runnable() {
			    public void run() {
			    	dateField.setText(date);
			    }
			});
		}

		@Override
		public void onUpdateIgnitionSate(final int state) {
			Log.d(TAG, "Ignition state is " + state);
			postToUI(new Runnable() {
			    public void run() {
			    	switch(state){
			    		case 0:
			    			// Pause the music as we exit the vehicle
			    			if(mPlayerBound && mCurrentRadioMode == RadioModes.AUX && mIsPlaying)
			    				mPlayerService.sendPauseKey();
			    			changeScreenState(false);
			    			break;
			    		case 1:
			    			changeScreenState(true);
			    			if(mPlayerBound && mCurrentRadioMode == RadioModes.AUX && !mIsPlaying)
			    				// Sleep for a second and then play the music again
								try {
									Thread.sleep(1000);
				    				mPlayerService.sendPlayKey();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
			    			break;
			    	}
			    }
			});
		}

		@Override
		public void onUpdateStreetLocation(final String streetName) {
			Log.d(TAG, "Street name " + streetName);
			postToUI(new Runnable() {
			    public void run() {
			    	geoStreetField.setText(streetName);
			    }
			});
		}
		
		@Override
		public void onUpdateLocale(final String cityName) {
			Log.d(TAG, "Locale name " + cityName);
			postToUI(new Runnable() {
			    public void run() {
			    	geoLocaleField.setText(cityName);
			    }
			});
		}

		@Override
		public void onUpdateGPSCoordinates(final String gpsCoordinates){
			Log.d(TAG, "GPS Coordinates are " + gpsCoordinates);
			postToUI(new Runnable(){
			    public void run() {
			    	geoCoordinatesField.setText(gpsCoordinates);
			    }
			});
		}
		
		@Override
		public void onUpdateGPSAltitude(final int altitude){
			Log.d(TAG, "Setting GPS Alitude");
			postToUI(new Runnable(){
			    public void run(){
			    	geoAltitudeField.setText(String.format("Alt.: %sm", altitude));
			    }
			});
		}

		@Override
		public void onUpdateGPSTime(final String time){
			Log.d(TAG, "Got GPS Time of " + time);
		}

		@Override
		public void onTrackFwd(){
			Log.d(TAG, "Changing the track fwd in callback due to steering input!");
			postToUI(new Runnable(){
			    public void run(){
					if(mPlayerBound)
						mPlayerService.sendNextKey();
			    }
			});
		}

		@Override
		public void onTrackPrev(){
			Log.d(TAG, "Changing the track fwd in callback due to steering input!");
			postToUI(new Runnable(){
			    public void run(){
					if(mPlayerBound)
						mPlayerService.sendPreviousKey();
			    }
			});
		}
		
		@Override
		public void onVoiceBtnPress(){
			// Repurpose this button to pause/play music
			Log.d(TAG, "Changing playback state in callback due to steering input!");
			postToUI(new Runnable(){
			    public void run(){
			    	if(mPlayerBound && mCurrentRadioMode == RadioModes.AUX){
			    		Log.d(TAG, "Firing off playback change as we are in AUX mode");
			    		if(mIsPlaying)
							mPlayerService.sendPauseKey();
						else
							mPlayerService.sendPlayKey();
			    	}
			    }
			});
		}

		@Override
		public void onUpdateRadioStatus(int status){
			Log.d(TAG, String.format("Radio is %s", status));
			// Radio is off, turn it on
			if(status == 0){
				postToUI(new Runnable(){
				    public void run(){
				    	try {
				    		sendIBusCommand(IBusCommandsEnum.BMToRadioPwrPress);
							Thread.sleep(500);
							sendIBusCommand(IBusCommandsEnum.BMToRadioPwrRelease);
						} catch (InterruptedException e) {
							// First world anarchy
						}
				    }
				});
			}
		}
		
	};
	
    private ServiceConnection mPlayerConnection = new ServiceConnection(){
    	
    	@Override
    	public void onServiceConnected(ComponentName className, IBinder service){
    		// Getting the binder and activating RemoteController instantly
    		Log.d(TAG, "Getting Music Player Binder object");
    		MusicControllerService.PlayerBinder binder = (MusicControllerService.PlayerBinder) service;
    		mPlayerService = binder.getService();
    		mPlayerService.enableController();
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
    			Log.d(TAG, "mIBusService is NOT NULL");
    			mIBusService.setCallbackListener(mIBusUpdateListener);
    			// Send a "get" request to populate the values on screen
    			// Do it here because this is when the service methods come into scope
    			if(mIBusBound){
    				sendIBusCommand(IBusCommandsEnum.BMToIKEGetTime);
    				sendIBusCommand(IBusCommandsEnum.BMToIKEGetDate);
    				sendIBusCommand(IBusCommandsEnum.BMToIKEGetFuel1);
    				sendIBusCommand(IBusCommandsEnum.BMToIKEGetFuel2);
    				sendIBusCommand(IBusCommandsEnum.BMToIKEGetOutdoorTemp);
    				sendIBusCommand(IBusCommandsEnum.BMToIKEGetRange);
    				sendIBusCommand(IBusCommandsEnum.BMToIKEGetAvgSpeed);
    				
    				/* This thread should make sure to send out and request
    				 * any IBus messages that the BM usually would.
    				 * We should also make sure to keep the radio in "Info"
    				 * mode at all times here. 
    				 */
    				new Thread(new Runnable() {
    					public void run() {
    						mLastRadioStatus = 0;
    						while(mIBusBound){
    							try{
	    							getActivity().runOnUiThread(new Runnable(){
	    								@Override
	    								public void run(){
	    									// When did the radio last update us? If more than ten seconds, ask info
	    									
	    									// BM Emulation
	    									// TODO Every 10 seconds send a RadioStatusRequest
	    									// TODO Respond to CD requests from Radio to support AUX
	    									
	    									long statusDiff = Calendar.getInstance().getTimeInMillis() - mLastRadioStatus;
	    									
	    									Log.d(TAG, String.format("Milliseconds since last Radio message: %s", statusDiff));
	    									
	    									if(statusDiff > 10000 && ! (mCurrentRadioMode == RadioModes.AUX)){
	    										Log.d(TAG, "Requesting Radio Info");
	    										try {
	    											sendIBusCommand(IBusCommandsEnum.BMToRadioInfoPress);
													Thread.sleep(500);
													sendIBusCommand(IBusCommandsEnum.BMToRadioInfoRelease);
												} catch (InterruptedException e) {
													// First world anarchy
												}
	    										
	    									}
	    								}
	    							});
	    							Thread.sleep(5000);
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
				mPlayerHandler.postDelayed(this, 100);
			}
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
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.main_activity, container, false);
		// Layouts
    	radioLayout = (LinearLayout) v.findViewById(R.id.radioAudio);
    	tabletLayout = (LinearLayout) v.findViewById(R.id.tabletAudio);
    	
    	
		// Keep a wake lock
    	changeScreenState(true);
		
    	// Bind required background services
    	bindServices();
    	
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
								mPlayerService.sendPauseKey();
							} else {
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
				mPlayerHandler.removeCallbacks(mUpdateSeekBar);
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mPlayerHandler.post(mUpdateSeekBar);
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

		// Set the action of each button
		btnVolUp.setTag(IBusCommandsEnum.BMToRadioVolumeUp.name());
		btnVolDown.setTag(IBusCommandsEnum.BMToRadioVolumeDown.name());
		btnRadioFM.setTag("BMToRadioFM");
		btnRadioAM.setTag("BMToRadioAM");
		btnPrev.setTag("BMToRadioTuneRev");
		btnNext.setTag("BMToRadioTuneFwd");

		mBtnMusicMode.setOnCheckedChangeListener(new OnCheckedChangeListener(){
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
		    	Log.d(TAG, "Changing Music Mode");
		        // Tablet Mode if checked, else Radio
		    	if(isChecked){
		    		radioLayout.setVisibility(View.GONE);
		    		tabletLayout.setVisibility(View.VISIBLE);
		    		// Send IBus Message
		    		if(! (mCurrentRadioMode == RadioModes.AUX))
		    			changeRadioMode(RadioModes.AUX);
		        }else{
		        	if(mIsPlaying)
		        		mPlayerService.sendPauseKey();
		    		radioLayout.setVisibility(View.VISIBLE);
		    		tabletLayout.setVisibility(View.GONE);
		    		// Send IBus Message
		    		if(mCurrentRadioMode == RadioModes.AUX || mCurrentRadioMode == RadioModes.CD)
		    			changeRadioMode(RadioModes.Radio);
		        }
		    }
		});

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
		//historicalAvgSpeedField.setOnLongClickListener(valueResetter);
		
		OnClickListener clickSingleAction = new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendIBusCommand(IBusCommandsEnum.valueOf(v.getTag().toString()));
			}
		};
		
		OnTouchListener touchAction = new OnTouchListener() {
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
		return v;
	}
	
	private void changeRadioMode(final RadioModes mode){
		new Thread(new Runnable() {
			public void run() {
				getActivity().runOnUiThread(new Runnable(){
					@Override
					public void run(){
						try {
							if(mode == RadioModes.AUX && !(mCurrentRadioMode== RadioModes.AUX)){
								Log.d(TAG, "Pressing Mode for AUX - Current Mode '" + mCurrentRadioMode + "'");
								sendIBusCommand(IBusCommandsEnum.BMToRadioModePress);
								Thread.sleep(250);
								sendIBusCommand(IBusCommandsEnum.BMToRadioModeRelease);
								Thread.sleep(1000);
								Log.d(TAG, "Mode now " + mCurrentRadioMode.toString());
								changeRadioMode(mode);
							}else if(mode == RadioModes.Radio && (mCurrentRadioMode != RadioModes.Radio)){
								Log.d(TAG, "Pressing Mode to get Radio - Current Mode '" + mCurrentRadioMode + "'");
								sendIBusCommand(IBusCommandsEnum.BMToRadioModePress);
								Thread.sleep(250);
								sendIBusCommand(IBusCommandsEnum.BMToRadioModeRelease);
								Thread.sleep(1000);
								Log.d(TAG, "Mode now " + mCurrentRadioMode.toString());
								changeRadioMode(mode);
							}
						} catch (InterruptedException e){
							// First world anarchy
						}
					}
				});
			}
		}).start();
	}
	
	/** TODO Make this actually work
	 * Acquire a screen wake lock to either turn the screen on or off
	 * @param screenOn if true, turn the screen on, else turn it off
	 */
	@SuppressWarnings("deprecation")
	private void changeScreenState(boolean screenOn){
		if(mPowerManager == null) mPowerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
		
		releaseWakelock();
		
		String state = (screenOn == true) ? "on" : "off";
		Log.d(TAG, "Screen is being turned " + state);
		WindowManager.LayoutParams layoutP = getActivity().getWindow().getAttributes();
		if(!screenOn){
			//layoutP.flags = LayoutParams.FLAG_KEEP_SCREEN_ON;
			layoutP.screenBrightness = -1;
			screenWakeLock = mPowerManager.newWakeLock(
			PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
				"screenWakeLock"
			);
			screenWakeLock.acquire();
		}else{
			layoutP.screenBrightness = 0;
			screenWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "screenWakeLock"); 
		}
		getActivity().getWindow().setAttributes(layoutP);
	}
	
	private void releaseWakelock(){
    	if(screenWakeLock != null){
    		if(screenWakeLock.isHeld()) screenWakeLock.release();
    	}
	}
	
	private void showToast(String toastText){
		Context appContext = getActivity();
		Toast.makeText(appContext, toastText, Toast.LENGTH_LONG).show();
	}
	
	private void sendIBusCommand(IBusCommandsEnum cmd, Object... args){
		if(mIBusBound){
			mIBusService.sendCommand(new IBusCommand(cmd, args));
		}
	}
	
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d(TAG, "onDestroy called");
    	releaseWakelock();
    	unbindServices();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	Log.d(TAG, "onPause called");
    	releaseWakelock();
    	unbindServices();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.d(TAG, "onResume called");
    	bindServices();
    }
}