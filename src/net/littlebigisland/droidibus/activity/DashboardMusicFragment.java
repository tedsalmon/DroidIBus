package net.littlebigisland.droidibus.activity;

import net.littlebigisland.droidibus.R;
import net.littlebigisland.droidibus.ibus.IBusCommandsEnum;
import net.littlebigisland.droidibus.ibus.IBusCallbackReceiver;
import net.littlebigisland.droidibus.ibus.IBusMessageService;
import net.littlebigisland.droidibus.ibus.IBusMessageService.IOIOBinder;
import net.littlebigisland.droidibus.music.MusicControllerService;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteController;
import android.media.RemoteController.MetadataEditor;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class DashboardMusicFragment extends BaseFragment{
	public String TAG = "DroidIBusMusicFragment";

	private IBusCallbackReceiver mIBusUpdateListener = new IBusCallbackReceiver(){
		
	};
	
	// Fields in the activity
	protected TextView stationText, radioRDSIndicatorField, radioProgramField,
					   radioBroadcastField, radioStereoIndicatorField;

	// Views in the Activity
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
	
	protected RadioModes mCurrentRadioMode = null; // Current Radio Text
	protected RadioTypes mRadioType = null; // Users radio type
	protected long mLastRadioStatus = 0; // Epoch of last time we got a status message from the Radio
	protected long mLastModeChange = 0; // Time that the radio mode last changed
	protected long mLastRadioStatusRequest = 0; // Time we last requested the Radio's status
	protected boolean mCDPlayerPlaying = false;
	
	protected enum RadioModes{
		AUX,
		CD,
		Radio
	}
	
	protected enum RadioTypes{
		BM53,
		CD53
	}
	
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
					e.printStackTrace();
				}
				sendIBusCommand(IBusCommandsEnum.BMToIKEGetTime);
				sendIBusCommand(IBusCommandsEnum.BMToIKEGetDate);
    		}
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        	Log.e(TAG, "mIBusService is disconnected");
            mIBusBound = false;
        }
    };
	
	private void bindServices() {
		serviceStarter(IBusMessageService.class, mIBusConnection);
	}
	
	private void unbindServices() {
		if(mIBusBound){
			mIBusService.disable();
			serviceStopper(IBusMessageService.class, mIBusConnection);
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setRetainInstance(true);
		final View v = inflater.inflate(R.layout.settings, container, false);
		return v;
	}
	
	@Override
	public void onActivityCreated (Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
		Log.d(TAG, "Settings: onActivityCreated Called");
		// Bind required background services last since the callback
		// functions depend on the view items being initialized
		if(!mIBusBound){
			bindServices();
		}
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    Log.d(TAG, "Settings: onResume Called");
	}

	@Override
	public void onPause() {
	    super.onPause();
	    Log.d(TAG, "Settings: onPause Called");
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		mIBusService.removeCallback(mIBusUpdateListener);
		Log.d(TAG, "Settings: onDestroy Called");
		if(mIBusBound){
			unbindServices();
		}
	}
}