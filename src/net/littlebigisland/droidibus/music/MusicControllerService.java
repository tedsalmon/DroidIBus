package net.littlebigisland.droidibus.music;
 
/**
 * @author Ted <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 *
 */
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.media.session.MediaSessionManager;
import android.media.session.MediaController;
import android.os.Binder;
import android.service.notification.NotificationListenerService;
import android.util.Log;


public class MusicControllerService extends NotificationListenerService{
    
    private static String TAG = "DroidIBus";
    private static final int ARTWORK_HEIGHT = 114;
    private static final int ARTWORK_WIDTH = 114;
    private Context mContext;
    // Callback provided by user

    /**
     * Return the MusicControllerService on bind
     * @return MusicControllerService instance
     */
    public class PlayerBinder extends Binder {
        public MusicControllerService getService() {
            return MusicControllerService.this;
        }
    }
    
    @Override
    public void onCreate() {
        // Saving the context for further reuse
        mContext = getApplicationContext();
        MediaSessionManager mediaManager = (MediaSessionManager) mContext.getSystemService(
            Context.MEDIA_SESSION_SERVICE
        );
        List<MediaController> controllers = mediaManager.getActiveSessions(
            new ComponentName(mContext, MusicControllerService.class)
        );
        Log.d(TAG, String.format("Found %s controllers", controllers.size()));
        Log.d(TAG, "Playback State " + controllers.get(0).getPlaybackState().toString());
    }
    
}