package net.littlebigisland.droidibus.music;
 
/**
 * MusicControllerService class
 * Handle communication between active Media Sessions and our Activity
 * by implementing Android "L" MusicControllerService class
 * @author Ted <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 *
 */
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSessionManager;
import android.media.session.MediaController;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.util.Log;
import android.view.KeyEvent;


public class MusicControllerService extends NotificationListenerService implements MediaSessionManager.OnActiveSessionsChangedListener{
    
    private static String TAG = "DroidIBus";
    private static String CTAG = "MediaControllerService: ";
    private Context mContext;
    private IBinder mBinder = new MusicControllerBinder();
    
    private List<MediaController> mMediaControllers = null;
    private MediaController mActiveMediaController = null;
    private MediaController.TransportControls mActiveMediaTransport = null;
    // Callback provided by user 
    private Map<Handler, MediaController.Callback> mClientCallbacks = new HashMap<Handler, MediaController.Callback>();
    
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
     * Returns the active media controller
     * @return MediaController
     */
    public MediaController getActiveMediaController(){
        return mActiveMediaController;
    }
    
    /**
     * Returns the TransportControls for the active media controller
     * @return MediaController.TransportControls
     */
    public MediaController.TransportControls getActiveMediaTransport(){
        return mActiveMediaTransport;
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
    
    /**
     * Return the MusicControllerBinder to any
     * activities that bind to this service
     * @return mBinder - The classes Binder
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    /**
     * Handle session disconnection
     */
    public void onSessionDisconnected(){
        Log.i(TAG, CTAG + "MediaSession disconnected");
        mMediaControllers.clear();
        refreshActiveMediaControllers();
        // Start the default player if no media sessions are active
        if(mMediaControllers.size() == 0){
            sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        }
    }
    
    /**
     * Register callback with the active media session
     * @param cb Clients callback class implementation
     * @param handle The handle to the clients thread
     */
    public void registerCallback(MediaController.Callback cb, Handler handle){
        mClientCallbacks.put(handle, cb);
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
            for(Handler handle: mClientCallbacks.keySet()){
                mActiveMediaController.registerCallback(
                    mClientCallbacks.get(handle),
                    handle
                );
            }
        }else{
            Log.e(TAG, CTAG + "Requested MediaSession not found!");
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
        // Default to the first session if we don't have one
        if(mActiveMediaController == null && mMediaControllers.size() > 0){
            setActiveMediaSession(mMediaControllers.get(0).getPackageName());
        }
    }
    
    @Override
    public void onActiveSessionsChanged(List<MediaController> mediaControllers){
        Log.i(TAG, CTAG + "System MediaSessions changed");
        setActiveMediaControllers(mediaControllers);
    }
    
    @Override
    public void onCreate(){
        Log.d(TAG, CTAG + "onCreate()");
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