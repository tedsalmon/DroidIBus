package net.littlebigisland.droidibus.activity;
/**
 * Control Fragment for IBus UI 
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus.activity
 */
import java.util.Calendar;

import net.littlebigisland.droidibus.R;
import net.littlebigisland.droidibus.ibus.IBusCommands;
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
import android.view.WindowManager.LayoutParams;
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
	private String TAG = "DroidIBus";
	
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
	protected MusicControllerService mPlayerService;
	protected boolean mPlayerBound = false;
	
	protected Handler mPlayerHandler = new Handler();

	protected boolean mIsPlaying = false;
	protected long mSongDuration = 1;
	
	protected Calendar time = Calendar.getInstance();
	
	protected PowerManager mPowerManager = null;
	protected WakeLock screenWakeLock;
	
	protected String currentRadioMode = ""; // Current Radio Text
	protected long lastRadioStatus = 0; // Epoch of last time we got a status message from the Radio

	private enum radioModes{
		AUX,
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
			// @TODO Merge with previous function, this is stupid
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
			    	lastRadioStatus = time.getTimeInMillis();
			    	currentRadioMode = text;
			    	stationText.setText(text);
			    }
			});
		}
		
		@Override
		public void onUpdateRadioBrodcasts(final String broadcastType){
			Log.d(TAG, "Setting Radio Broadcast Type");
			postToUI(new Runnable(){
			    public void run(){
			    	lastRadioStatus = time.getTimeInMillis();
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
			    		lastRadioStatus = time.getTimeInMillis();
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
			    	lastRadioStatus = time.getTimeInMillis();
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
			    	lastRadioStatus = time.getTimeInMillis();
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
		public void onUpdateRange(final int range){
			Log.d(TAG, "Setting Gas Range in Callback!");
			postToUI(new Runnable() {
			    public void run() {
			    	rangeField.setText(Integer.toString(range));
			    }
			});
		}
		
		@Override
		public void onUpdateOutdoorTemp(final int temp){
			Log.d(TAG, "Setting Outdoor Temp in Callback!");
			postToUI(new Runnable() {
			    public void run() {
			    	outTempField.setText(Integer.toString(temp));
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
		public void onUpdateIgnitionSate(int state) {
			Log.d(TAG, "Ignition state is " + state);
			switch(state){
				case 0:
					changeScreenState(false);
					break;
				case 1:
					changeScreenState(true);
					break;
			}
			
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
		public void onUpdateGPSAltitude(final int altitude){
			Log.d(TAG, "Setting GPS Alitude");
			postToUI(new Runnable(){
			    public void run(){
			    	geoAltitudeField.setText(String.format("Altitude: %s", altitude));
			    }
			});
		}

		@Override
		public void onUpdateGPSTime(final String time){
			Log.d(TAG, "Got GPS Time of " + time);
		}

		@Override
		public void onUpdateRadioStatus(int status){
			// TODO Track status
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
        	Log.e("DroidIBus", "MusicPlayerService is disconnected");
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
    			Log.d("DroidIBus", "mIBusService is NOT NULL");
    			mIBusService.setCallbackListener(mIBusUpdateListener);
    			// Send a "get" request to populate the values on screen
    			// Do it here because this is when the service methods come into scope
    			if(mIBusBound){
    				sendIBusCommand(IBusCommands.BMToIKEGetTime);
    				sendIBusCommand(IBusCommands.BMToIKEGetDate);
    				sendIBusCommand(IBusCommands.BMToIKEGetFuel1);
    				sendIBusCommand(IBusCommands.BMToIKEGetFuel2);
    				sendIBusCommand(IBusCommands.BMToIKEGetRange);
    				sendIBusCommand(IBusCommands.BMToIKEGetAvgSpeed);
    				sendIBusCommand(IBusCommands.BMToIKEGetOutdoorTemp);
    				
    				/* This thread should make sure to send out and request
    				 * any IBus messages that the BM usually would.
    				 * We should also make sure to keep the radio in "Info"
    				 * mode at all times here. 
    				 */
    				new Thread(new Runnable() {
    					public void run() {
    						while(mIBusBound){
    							try{
	    							getActivity().runOnUiThread(new Runnable(){
	    								@Override
	    								public void run(){
	    									// When did the radio last update us? If more than ten seconds, ask info
	    									
	    									// BM Emulation
	    									// TODO Every 10 seconds send a RadioStatusRequest
	    									// TODO Respond to CD requests from Radio to support AUX
	    									
	    									if( (time.getTimeInMillis() - lastRadioStatus) > 10000 ){
	    										Log.d(TAG, "Requesting Radio Status");
	    										try {
	    											sendIBusCommand(IBusCommands.BMToRadioInfoPress);
													Thread.sleep(500);
													sendIBusCommand(IBusCommands.BMToRadioInfoPress);
												} catch (InterruptedException e) {
													// First world anarchy
												}
	    										
	    									}
	    								}
	    							});
	    							Thread.sleep(2000);
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
        	Log.e("DroidIBus", "mIBusService is disconnected");
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
		Switch btnMusicMode = (Switch) v.findViewById(R.id.btnMusicMode);
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
		btnVolUp.setTag(IBusCommands.BMToRadioVolumeUp.name());
		btnVolDown.setTag(IBusCommands.BMToRadioVolumeDown.name());
		btnRadioFM.setTag("BMToRadioFM");
		btnRadioAM.setTag("BMToRadioAM");
		btnPrev.setTag("BMToRadioTuneRev");
		btnNext.setTag("BMToRadioTuneFwd");

		btnMusicMode.setOnCheckedChangeListener(new OnCheckedChangeListener(){
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
		        // Tablet Mode if checked, else Radio
		    	if(isChecked){
		    		radioLayout.setVisibility(View.GONE);
		    		tabletLayout.setVisibility(View.VISIBLE);
		    		// Send IBus Message
		    		if(!currentRadioMode.equals("AUX"))
		    			changeRadioMode(radioModes.AUX);
		        }else{
		        	if(mIsPlaying)
		        		mPlayerService.sendPauseKey();
		    		radioLayout.setVisibility(View.VISIBLE);
		    		tabletLayout.setVisibility(View.GONE);
		    		// Send IBus Message
		    		if(currentRadioMode.equals("AUX") || currentRadioMode.equals("NO CD"))
		    			changeRadioMode(radioModes.Radio);
		        }
		    }
		});

		// Set the long press of values for IKE resets
		OnLongClickListener valueResetter = new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				IBusCommands action = IBusCommands.valueOf(v.getTag().toString());
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

		fuel1Field.setTag(IBusCommands.BMToIKEResetFuel1.name());
		fuel2Field.setTag(IBusCommands.BMToIKEResetFuel1.name());
		avgSpeedField.setTag(IBusCommands.BMToIKEResetAvgSpeed.name());

		fuel1Field.setOnLongClickListener(valueResetter);
		fuel2Field.setOnLongClickListener(valueResetter);
		avgSpeedField.setOnLongClickListener(valueResetter);
		//historicalAvgSpeedField.setOnLongClickListener(valueResetter);
		
		OnClickListener clickSingleAction = new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendIBusCommand(IBusCommands.valueOf(v.getTag().toString()));
			}
		};
		
		OnTouchListener touchAction = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				String action = (event.getAction() == MotionEvent.ACTION_DOWN) ? "Press" : "Release";
				sendIBusCommand(IBusCommands.valueOf(v.getTag().toString() + action));
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
	
	private void changeRadioMode(final radioModes mode){
		new Thread(new Runnable() {
			public void run() {
				getActivity().runOnUiThread(new Runnable(){
					@Override
					public void run(){
						try {
							if(mode == radioModes.AUX){
								if(!currentRadioMode.equals("AUX")){
									Log.d(TAG, "Pressing Mode for AUX - Current Mode '" + currentRadioMode + "'");
									sendIBusCommand(IBusCommands.BMToRadioModePress);
									Thread.sleep(250);
									sendIBusCommand(IBusCommands.BMToRadioModeRelease);
									Thread.sleep(1000);
									Log.d(TAG, "Mode now " + currentRadioMode);
									changeRadioMode(mode);
								}
							}else if(mode == radioModes.Radio){
								if(currentRadioMode.equals("AUX") || currentRadioMode.equals("NO CD")){
									Log.d(TAG, "Pressing Mode to get Radio - Current Mode '" + currentRadioMode + "'");
									sendIBusCommand(IBusCommands.BMToRadioModePress);
									Thread.sleep(250);
									sendIBusCommand(IBusCommands.BMToRadioModeRelease);
									Thread.sleep(1000);
									Log.d(TAG, "Mode now " + currentRadioMode);
									changeRadioMode(mode);
								}
							}
						} catch (InterruptedException e){
							// First world anarchy
						}
					}
				});
			}
		}).start();
	}
	
	/**
	 * Acquire a screen wake lock to either turn the screen on or off
	 * @param screenOn if true, turn the screen on, else turn it off
	 */
	private void changeScreenState(boolean screenOn){
		if(mPowerManager == null)
			mPowerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
		
		if(screenWakeLock != null)
			if(screenWakeLock.isHeld())
				screenWakeLock.release();
		String state = (screenOn == true) ? "on" : "off";
		Log.d(TAG, "Screen is being turned " + state);
		int lockType = (screenOn == true) ? LayoutParams.FLAG_KEEP_SCREEN_ON : PowerManager.PARTIAL_WAKE_LOCK;
    	screenWakeLock = mPowerManager.newWakeLock(lockType, "screenWakeLock");
    	screenWakeLock.acquire();
	}
	
	private void showToast(String toastText){
		Context appContext = getActivity();
		Toast.makeText(appContext, toastText, Toast.LENGTH_LONG).show();
	}
	
	private void sendIBusCommand(IBusCommands cmd){
		if(mIBusBound){
			mIBusService.sendCommand(cmd);
		}
	}
	
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d(TAG, "onDestroy called");
    	screenWakeLock.release();
    	unbindServices();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	Log.d(TAG, "onPause called");
    	unbindServices();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.d(TAG, "onResume called");
    	bindServices();
    }
}