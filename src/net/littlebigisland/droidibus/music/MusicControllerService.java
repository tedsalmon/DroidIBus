package net.littlebigisland.droidibus.music;
 
/**
 * @author Ted <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 *
 */
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSessionManager;
import android.media.session.MediaController;
import android.os.Binder;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.util.Log;
import android.view.KeyEvent;


public class MusicControllerService extends NotificationListenerService implements MediaSessionManager.OnActiveSessionsChangedListener{
    
    private static String TAG = "DroidIBus";
    private static String CTAG = "MediaControllerService: ";
    //private static final int ARTWORK_HEIGHT = 114;
    //private static final int ARTWORK_WIDTH = 114;
    private Context mContext;
    
    private List<MediaController> mMediaControllers = null;
    private MediaController mActiveMediaController = null;
    private MediaController.TransportControls mActiveMediaTransport = null;
    // Callback provided by user 
    //private List<MediaController.Callback> mClientCallbacks = new ArrayList<MediaController.Callback>();
    

    /**
     * Return the MusicControllerService on bind
     * @return MusicControllerService instance
     */
    public class MusicControllerBinder extends Binder{
        public MusicControllerService getService(){
            return MusicControllerService.this;
        }
    }

    /**
     * Returns a list of active sessions we have registered
     * @return List of available sessions
     */
    public String[] getAvailableMediaSessions(){
        String[] sessionNames = new String[mMediaControllers.size()];
        int index = 0;
        for (MediaController controller : mMediaControllers){
            sessionNames[index] = controller.getPackageName();
            index++;
        }
        return sessionNames;
    }
    
    public MediaController.TransportControls getActiveMediaTransport(){
        return mActiveMediaTransport;
    }
    
    /**
     * Switch the active media session to the given session name 
     * @param sessionName The string name of the session to switch to
     */
    public void setActiveMediaSession(String sessionName){
        MediaController newSession = null;
        for (MediaController controller : mMediaControllers){
            if(sessionName == controller.getPackageName()){
                newSession = controller;
            }
        }
        if(newSession != null){
            mActiveMediaController = newSession;
            mActiveMediaTransport = newSession.getTransportControls();
        }else{
            Log.e(TAG, CTAG + "Requested MediaSession not found!");
        }
    }
    
    public void registerCallback(MediaController.Callback cb, Handler handle){
        if(mActiveMediaController != null){
            mActiveMediaController.registerCallback(cb, handle);            
        }else{
            Log.e(
                TAG,
                CTAG + "Attempted to registerCallback for a null active session"
            );
        }
    }
    
    /**
     * Return the media session manager from the system
     * @return MediaSessionManager
     */
    private MediaSessionManager getMediaSessionManager(){
        return (MediaSessionManager) mContext.getSystemService(
            Context.MEDIA_SESSION_SERVICE
        );
    }
    
    /**
     * Query the system for actively registered MediaSessions
     * and store them for future use
     */
    private void refreshActiveMediaControllers(){
        MediaSessionManager mediaManager = getMediaSessionManager();
        setActiveMediaControllers(mediaManager.getActiveSessions(
            new ComponentName(mContext, MusicControllerService.class)
        ));
    }
    
    /**
     * Send the ACTION_UP and ACTION_DOWN key events
     * @param keyCode The key we are pressing
     */
    private void sendKeyEvent(int keyCode){
        KeyEvent keyDown  = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        KeyEvent keyUp  = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
        
        Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        downIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyDown);
        
        Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyUp);
        
        mContext.sendOrderedBroadcast(downIntent, null);
        mContext.sendOrderedBroadcast(upIntent, null);
    }
    
    /**
     * Store and report the MediaControllers found
     * @param mediaControllers List of MediaControllers to store
     */
    private void setActiveMediaControllers(List<MediaController> mediaControllers){
        Log.d(TAG, CTAG + "Sessions Changed");
        mMediaControllers = mediaControllers;
        for (MediaController controller : mediaControllers){
            Log.i(
                TAG,
                String.format(
                    CTAG + "Found MediaSession for package %s with state %s",
                    controller.getPackageName(),
                    controller.getPlaybackState().toString()
                )
            );
        }
    }
    
    @Override
    public void onActiveSessionsChanged(List<MediaController> mediaControllers){
        Log.d(TAG, "MediaControllerService: System MediaSessions changed");
        setActiveMediaControllers(mediaControllers);
    }
    
    @Override
    public void onCreate(){
        Log.d(TAG, "MusicControllerService onCreate()");
        // Saving the context for further reuse
        mContext = getApplicationContext();
        // Get the session manager and register
        // us to listen for new sessions
        getMediaSessionManager().addOnActiveSessionsChangedListener(
            this, 
            new ComponentName(mContext, MusicControllerService.class)
        );
        // Get the current active media sessions
        refreshActiveMediaControllers();
        // Handle no active sessions
        if(mMediaControllers.size() == 0){
            sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        }
    }
    
}