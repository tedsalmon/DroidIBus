package net.littlebigisland.droidibus.services;
 
/**
 * MusicControllerService class
 * Handle communication between active Media Sessions and our Activity
 * by implementing Android "L" MusicControllerService class
 * @author Ted <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 *
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController.Callback;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.media.session.PlaybackState.Builder;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.util.Log;
import android.view.KeyEvent;


public class MusicControllerService extends NotificationListenerService implements MediaSessionManager.OnActiveSessionsChangedListener{
    
    private static String TAG = "DroidIBus";
    private static String CTAG = "MediaControllerService: ";
    private Context mContext = null;
    private IBinder mBinder = new MusicControllerBinder();
    
    Map<String, MediaController> mMediaControllers = new HashMap<String, MediaController>();
    private MediaController mActiveMediaController = null;
    private MediaController.TransportControls mActiveMediaTransport = null;

    Map<String, String> mMediaControllerSessionMap = new HashMap<String, String>();
    
    // Callback provided by user 
    private Map<Handler, MediaController.Callback> mClientCallbacks = new HashMap<Handler, MediaController.Callback>();
    
    // Application state variables
    private int mPlaybackState = 0;
    
    /**
     * Return the MusicControllerService on bind
     * @return MusicControllerService instance
     */
    public class MusicControllerBinder extends Binder{
        public MusicControllerService getService(){
            return MusicControllerService.this;
        }
    }
    
    public class Test extends MediaSession.Callback{
        
    }
    
    /**
     * Returns a simple boolean letting you know if a player is active
     * @return Boolean true if playing, false if not
     */
    public boolean getIsPlaying(){
        PlaybackState state = getMediaPlaybackState();
        return (state.getState() == PlaybackState.STATE_PLAYING);
    }
    
    /**
     * Returns the active media controller
     * @return MediaController
     */
    public MediaController getMediaController(){
        return mActiveMediaController;
    }
    
    /**
     * Returns the TransportControls for the active media controller
     * @return MediaController.TransportControls
     */
    public MediaController.TransportControls getMediaTransport(){
        return mActiveMediaTransport;
    }

    /**
     * Returns a list of active sessions we have registered
     * @return List of available sessions
     */
    public String[] getMediaSessions(){
        String[] sessionNames = new String[mMediaControllers.size()];
        int index = 0;
        for (MediaController controller : mMediaControllers.values()){
            sessionNames[index] = controller.getPackageName();
            index++;
        }
        return sessionNames;
    }
    
    /**
     * Returns the Metadata from the active media controller
     */
    public MediaMetadata getMediaMetadata(){
        if(mActiveMediaController != null){
            return mActiveMediaController.getMetadata();
        }
        return null;
    }
    
    /**
     * Returns the Playback state of the active media controller
     * @return PlaybackState object
     */
    public PlaybackState getMediaPlaybackState(){
        if(mActiveMediaController != null){
            return mActiveMediaController.getPlaybackState();
        }
        Builder mState = new PlaybackState.Builder();
        mState.setState(mPlaybackState, 0, 0);
        return mState.build();
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
     * Register callback with the active media session
     * @param cb Clients callback class implementation
     * @param handle The handle to the clients thread
     */
    public void registerCallback(MediaController.Callback cb, Handler handle){
        mClientCallbacks.put(handle, cb);
        if(mActiveMediaController != null){
            Log.d(TAG, CTAG + "Registering callback to active controller");
            mActiveMediaController.registerCallback(cb, handle);            
        }else{
            Log.e(TAG, CTAG + "Attempted to registerCallback on a null session");
        }
    }
    
    /**
     * Switch the active media session to the given session name 
     * @param sessionName The string name of the session to switch to
     */
    public void setMediaSession(String sessionName){
        MediaController newSession = null;
        String sessionToken = mMediaControllerSessionMap.get(sessionName);
        if(sessionToken != null){
            newSession = mMediaControllers.get(sessionToken);
            String oldSessionToken = "";
            if(mActiveMediaController != null){
                oldSessionToken = mActiveMediaController.getSessionToken().toString();
            }
            // Only take action if the active session isn't the current one
            if(newSession != null && !oldSessionToken.equals(sessionToken)){
                Log.d(TAG, CTAG + "Switching to session " + sessionName);
                for(Handler handle: mClientCallbacks.keySet()){
                    MediaController.Callback cb = mClientCallbacks.get(handle);
                    if(mActiveMediaController != null){
                        // Pause the music, if it isn't already
                        mActiveMediaTransport.pause();
                        // Unregister the old callback
                        mActiveMediaController.unregisterCallback(cb);
                    }
                    // Register the new callback
                    newSession.registerCallback(cb, handle);
                }
                mActiveMediaController = newSession;
                mActiveMediaTransport = newSession.getTransportControls();
                String packageName = mActiveMediaController.getPackageName();
                Log.d(
                    TAG, String.format(CTAG + "Session set to %s", packageName)
                );
            }
        }else{
            Log.e(TAG, CTAG + "Requested MediaSession not found!");
        }
    }
    
    /**
     * Unregister the given callback from the active Media Controller
     * @param cb Clients callback class implementation
     */
    public void unregisterCallback(MediaController.Callback cb){
        Log.d(TAG, CTAG + "Unregistering Callback via unregisterCallback()");
        mClientCallbacks.values().remove(cb);
        if(mActiveMediaController != null){
            mActiveMediaController.unregisterCallback(cb);
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
     * Pause the active media player or 
     * send the Pause KeyEvent to the system
     * if there are no active media players
     */
    public void pause(){
        if(mActiveMediaTransport == null){
            sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE);
        }else{
            mActiveMediaTransport.pause();
            mPlaybackState = PlaybackState.STATE_PAUSED;
            triggerPlaybackStateCallbacks();
        }
    }
    
    /**
     * Play the active media player or send 
     * the Play KeyEvent to the system if
     * there are no active media players
     */
    public void play(){
        if(mActiveMediaTransport == null){
            sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY);
        }else{
            mActiveMediaTransport.play();
            mPlaybackState = PlaybackState.STATE_PLAYING;
            triggerPlaybackStateCallbacks();
        }
    }
    
    /**
     * Query the system for actively registered MediaSessions
     * and store them for future use
     */
    private void refreshMediaControllers(){
        setMediaControllers(getMediaSessionManager().getActiveSessions(
            new ComponentName(mContext, MusicControllerService.class)
        ));
    }
    
    /**
     * Seek the active media player or send 
     * the KeyEvent Play to the system then
     * call the same method again
     */
    public void seekTo(long millis){
        if(mActiveMediaTransport == null){
            sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY);
            seekTo(millis);
        }else{
            mActiveMediaTransport.seekTo(millis);
        }
    }
    
    /**
     * Send the ACTION_UP and ACTION_DOWN key events
     * @param keyCode The key we are pressing
     */
    public void sendKeyEvent(int keyCode){
        Log.d(
            TAG,
            CTAG + "Sending KeyEvent -> " + KeyEvent.keyCodeToString(keyCode)
        );
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
    private void setMediaControllers(List<MediaController> mediaControllers){
        Log.d(TAG, CTAG + "Sessions Changed");
        List<String> currentSessions = new ArrayList<String>();
        for (MediaController remote: mediaControllers){
            String sToken = remote.getSessionToken().toString();
            String sName = remote.getPackageName();
            PlaybackState sPlayState = remote.getPlaybackState();
            currentSessions.add(sToken);
            if(!mMediaControllers.containsKey(sToken)){
                mMediaControllers.put(sToken, remote);
                mMediaControllerSessionMap.put(sName, sToken);
            }
            // Make sure if there's a session playing
            // that we set it to be the active session
            if(sPlayState != null){
                if(sPlayState.getState() == PlaybackState.STATE_PLAYING){
                    setMediaSession(sName);
                }
            }
            Log.i(
                TAG,
                String.format(
                    CTAG + "Found MediaSession for package %s" +
                    " with state %s and token %s",
                    sName,
                    sPlayState,
                    sToken
                )
            );
        }
        
        // Remove Controllers that aren't active
        Iterator<String> sessionTokens = mMediaControllers.keySet().iterator();
        while(sessionTokens.hasNext()){
            String sToken = sessionTokens.next();
            if(!currentSessions.contains(sToken)){
                Log.d(TAG, CTAG + "Removing session token " + sToken);
                sessionTokens.remove();
                mMediaControllerSessionMap.values().remove(sToken);
            }
        }
        if(mMediaControllers.isEmpty()){
            // No media controllers - Nullify our active session
            Log.d(TAG, CTAG + "No sessions available - Nullify objects");
            mActiveMediaController = null;
            mActiveMediaTransport = null;
        }else{
            // Default to a session if only one exists
            if(mActiveMediaController == null || mMediaControllers.size() == 1){
                setMediaSession(
                    mMediaControllers.get(currentSessions.get(0)).getPackageName()
                );
            }
        }
    }
    
    /**
     * Skip the active media player to the next song or 
     * send the Skip next KeyEvent to the system if
     * there are no active media players
     */
    public void skipToNext(){
        if(mActiveMediaTransport == null){
            sendKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
        }else{
            mActiveMediaTransport.skipToNext();
        }
    }
    
    /**
     * Skip the active media player to the last song or 
     * send the Skip next KeyEvent to the system if
     * there are no active media players
     */
    public void skipToPrevious(){
        if(mActiveMediaTransport == null){
            sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        }else{
            mActiveMediaTransport.skipToPrevious();
        }
    }
    
    /**
     * Toggle the music Playback state
     */
    public void togglePlayback(){
        if(getIsPlaying()){
            pause();
        }else{
            play();
        }
    }
    
    /**
     * Because some media players are still back about this,
     * manually trigger the calls to onPlaybackStateChanged()
     */
    private void triggerPlaybackStateCallbacks(){
        final PlaybackState mState = getMediaPlaybackState();
        for(Handler handler: mClientCallbacks.keySet()){
            final Callback mc = mClientCallbacks.get(handler);
            handler.post(new Runnable(){
                @Override
                public void run(){
                    mc.onPlaybackStateChanged(mState);
                }
            });
        }
    }
    
    @Override
    public void onActiveSessionsChanged(List<MediaController> mediaControllers){
        Log.i(TAG, CTAG + "System MediaSessions changed");
        setMediaControllers(mediaControllers);
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
        refreshMediaControllers();
    }
    
    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, CTAG + "onDestroy()");
        // Unregister ALL callbacks
        for(MediaController.Callback cb: mClientCallbacks.values()){
            unregisterCallback(cb);
        }
    }
}