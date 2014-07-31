package net.littlebigisland.droidibus.music;
 
/**
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 *
 */
 
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RemoteController;
import android.media.RemoteController.MetadataEditor;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.KeyEvent;
 
 
public class MusicControllerService extends NotificationListenerService implements RemoteController.OnClientUpdateListener {
	
	private static final int ARTWORK_HEIGHT = 114;
	private static final int ARTWORK_WIDTH = 114;
	
	private IBinder mBinder = new PlayerBinder();
	
	private RemoteController mAudioController;
	private Context mContext;
	
	// Callback provided by user
	private RemoteController.OnClientUpdateListener mExternalUpdateListener;
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	// We don't totally care about these for now, ignore
	@Override
	public void onNotificationPosted(StatusBarNotification notification) {
	}
	
	@Override
	public void onNotificationRemoved(StatusBarNotification notification) {
	}
	
	@Override
	public void onCreate() {
		// Saving the context for further reuse
		mContext = getApplicationContext();
		mAudioController = new RemoteController(mContext, this);
	}
	
	@Override
	public void onDestroy() {
		disableController();
	}
	
	/**
	 * Registers this service with the system audio service. Called via IBinder from the Activity
	 */
	public void enableController() {
		if(!((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).registerRemoteController(mAudioController)) {
			throw new RuntimeException("Error while registering RemoteController!");
		}else{
			mAudioController.setArtworkConfiguration(ARTWORK_WIDTH, ARTWORK_HEIGHT);
			setSynchronizationMode(mAudioController, RemoteController.POSITION_SYNCHRONIZATION_CHECK);
		}
	}
	
	/**
	 * Unregister from the system audio service.
	 */
	public void disableController() {
		((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).unregisterRemoteController(mAudioController);
	}
	
	/**
	 * Sets up external callback for client update events.
	 * @param listener External callback.
	 */
	public void setCallbackListener(RemoteController.OnClientUpdateListener listener) {
		mExternalUpdateListener = listener;
	}
	
	/**
	 * Sends "next" media key press.
	 */
	public void sendNextKey() {
		sendKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
	}
	
	/**
	 * Sends "previous" media key press.
	 */
	public void sendPreviousKey() {
		sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
	}
	
	/**
	 * Sends "pause" media key press, or, if player ignored this button, "play/pause".
	 */
	public void sendPauseKey() {
		if(!sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)) {
			sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
		}
	}
	
	/**
	 * Sends "play" button press, or, if player ignored it, "play/pause".
	 */
	public void sendPlayKey() {
		if(!sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)) {
			sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
		}
	}
	
	/**
	 * @return Current song position in milliseconds.
	 */
	public long getEstimatedPosition() {
		return mAudioController.getEstimatedMediaPosition();
	}
	
	/**
	 * Seeks to given position.
	 * @param ms Position in milliseconds.
	 */
	public void seekTo(long ms) {
		mAudioController.seekTo(ms);
	}
	
	// This method let us avoid the bug in RemoteController
	// which results in Exception when calling RemoteController#setSynchronizationMode(int)
	// doesn't seem to work though
	private void setSynchronizationMode(RemoteController controller, int sync) {
		if ((sync != RemoteController.POSITION_SYNCHRONIZATION_NONE) && (sync != RemoteController.POSITION_SYNCHRONIZATION_CHECK)) {
	        throw new IllegalArgumentException("Unknown synchronization mode " + sync);
	    }
	
		Class<?> iRemoteControlDisplayClass;
	
		try {
			iRemoteControlDisplayClass  = Class.forName("android.media.IRemoteControlDisplay");
		} catch (ClassNotFoundException e1) {
			throw new RuntimeException("Class IRemoteControlDisplay doesn't exist, can't access it with reflection");
		}
	
		Method remoteControlDisplayWantsPlaybackPositionSyncMethod;
		try {
			remoteControlDisplayWantsPlaybackPositionSyncMethod = AudioManager.class.getDeclaredMethod("remoteControlDisplayWantsPlaybackPositionSync", iRemoteControlDisplayClass, boolean.class);
			remoteControlDisplayWantsPlaybackPositionSyncMethod.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Method remoteControlDisplayWantsPlaybackPositionSync() doesn't exist, can't access it with reflection");
		}
	
		Object rcDisplay;
		Field rcDisplayField;
		try {
			rcDisplayField = RemoteController.class.getDeclaredField("mRcd");
			rcDisplayField.setAccessible(true);
			rcDisplay = rcDisplayField.get(mAudioController);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Field mRcd doesn't exist, can't access it with reflection");
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Field mRcd can't be accessed - access denied");
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Field mRcd can't be accessed - invalid argument");
		}
	
		AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
		try {
			remoteControlDisplayWantsPlaybackPositionSyncMethod.invoke(am, iRemoteControlDisplayClass.cast(rcDisplay), true);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Method remoteControlDisplayWantsPlaybackPositionSync() invocation failure - access denied");
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Method remoteControlDisplayWantsPlaybackPositionSync() invocation failure - invalid arguments");
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Method remoteControlDisplayWantsPlaybackPositionSync() invocation failure - invalid invocation target");
		}
	}
	
	/**
	 * Send the ACTION_UP and ACTION_DOWN key events
	 * @param keyCode The key we are pressing
	 * @return true if both clicks were delivered, else false
	 */
	private boolean sendKeyEvent(int keyCode) {
		KeyEvent keyDown  = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
		KeyEvent keyUp  = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
		boolean keyDownRes = mAudioController.sendMediaKeyEvent(keyDown);
		boolean keyUpRes = mAudioController.sendMediaKeyEvent(keyUp);
		if(! keyDownRes && ! keyUpRes){
			Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
			downIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyDown);
			getBaseContext().sendOrderedBroadcast(downIntent, null);
			Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
			upIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyUp);
			getBaseContext().sendOrderedBroadcast(downIntent, null);
		}
		return keyDownRes && keyUpRes;
	}
	
	/**
	 * Return the MusicControllerService on service bind
	 * @return MusicControllerService instance
	 */
	public class PlayerBinder extends Binder {
	    public MusicControllerService getService() {
	        return MusicControllerService.this;
	    }
	}
	
	/*
	 * Begin implementing the external callback handler.
	 * We simply pass the data over to the callback if the method is implemented
	 *  
	 */
	
	/**
	 * Called on change. True will be passed if the Metadata must be cleared
	 * @param hasChange	Is there a change?
	 * @see android.media.RemoteController.OnClientUpdateListener#onClientChange(boolean)
	 */
	@Override
	public void onClientChange(boolean hasChange) {
		if(mExternalUpdateListener != null) {
			mExternalUpdateListener.onClientChange(hasChange);
		}
	}
	
	/**
	 * Called on metadata change
	 * @param metaData Abstract data structure with song Metadata
	 */
	@Override
	public void onClientMetadataUpdate(MetadataEditor metaData) {
		if(mExternalUpdateListener != null) {
			mExternalUpdateListener.onClientMetadataUpdate(metaData);
		}
	}
	
	@Override
	public void onClientPlaybackStateUpdate(int arg0) {
		if(mExternalUpdateListener != null) {
			mExternalUpdateListener.onClientPlaybackStateUpdate(arg0);
		}
	}
	
	@Override
	public void onClientPlaybackStateUpdate(int arg0, long arg1, long arg2, float arg3) {
		if(mExternalUpdateListener != null) {
			mExternalUpdateListener.onClientPlaybackStateUpdate(arg0, arg1, arg2, arg3);
		}
	}
	
	@Override
	public void onClientTransportControlUpdate(int arg0) {
		if(mExternalUpdateListener != null) {
			mExternalUpdateListener.onClientTransportControlUpdate(arg0);
		}
	
	}
}