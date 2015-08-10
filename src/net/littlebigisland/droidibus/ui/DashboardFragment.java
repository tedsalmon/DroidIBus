package net.littlebigisland.droidibus.ui;
/**
 * Base Dashboard Fragment - Controls base functions
 * and drops in the child fragments 
 * @author Ted <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus.activity
 */
import net.littlebigisland.droidibus.R;
import net.littlebigisland.droidibus.ibus.IBusSystem;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public class DashboardFragment extends BaseFragment{
    
    public static final int SCREEN_ON = 1;
    public static final int SCREEN_OFF = 0;
    
    protected SharedPreferences mSettings = null;
    
    protected boolean mScreenOn = false;
    
    protected boolean mPopulatedFragments = false;
    
    IBusSystem.Callbacks mIBusCallbacks = new IBusSystem.Callbacks(){

        /* Callback to handle Ignition State Updates
         * @param int State of Ignition (0, 1, 2)
         */
        @Override
        public void onUpdateIgnitionSate(final int state){
            int screenState = (state > 0) ? SCREEN_ON : SCREEN_OFF;
            setScreenState(screenState);
        }

    };

    /**
     * Acquire a screen wake lock to either turn the screen on or off
     * @param screenState if true, turn the screen on, else turn it off
     */
    public void setScreenState(int screenState){
        Window window = getActivity().getWindow();
        WindowManager.LayoutParams layoutP = window.getAttributes();
        int screenParams = WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        
        if(screenState == SCREEN_ON && !mScreenOn){
            Log.d(TAG, CTAG + "Acquiring WakeLock");
            mScreenOn = true;
            layoutP.screenBrightness = -1;
            window.addFlags(screenParams);
        }
        
        if(screenState == SCREEN_OFF && mScreenOn){
            Log.d(TAG, CTAG + "Shutting the screen off");
            mScreenOn = false;
            window.clearFlags(screenParams);
            layoutP.screenBrightness = 0;
        }
        
        window.setAttributes(layoutP);
    }
    
    @Override
    public View onCreateView(
        LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ){
        final View v = inflater.inflate(R.layout.dashboard, container, false);
        Log.d(TAG, CTAG + "onCreateView()");
        if(!mPopulatedFragments){
            FragmentTransaction tx = getChildFragmentManager(
            ).beginTransaction();
            tx.add(R.id.music_fragment, new DashboardMusicFragment());
            tx.add(R.id.stats_fragment, new DashboardStatsFragment());
            tx.commit();
            mPopulatedFragments = true;
        }
        // Keep a wake lock
        setScreenState(SCREEN_ON);
        return v;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, CTAG + "onActivityCreated()");
        startIBusMessageService();
    }
    
    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, CTAG + "onDestroy()");
        setScreenState(SCREEN_OFF);
        stopIBusMessageService();
    }
}