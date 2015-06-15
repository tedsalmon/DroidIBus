package net.littlebigisland.droidibus.activity;
/**
 * 
 * @author Ted <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 *
 */
import net.littlebigisland.droidibus.R;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends Activity {
    private DrawerLayout mDrawerLayout = null;
    private ListView mDrawerList = null;
    private ArrayAdapter<String> mDrawerListAdapter = null;
    private ActionBarDrawerToggle mDrawerToggle;
    
    private Fragment[] mFragments = {
        new DashboardFragment(),
	null,
        new SettingsFragment(),
    };
    
    private String[] mFragmentTitles = {
        "Dashboard",
        "Navigation",
        "Settings"
    };
    
    // This callback sets the fragment title every time the fragment changes
    private OnBackStackChangedListener mBackstackStateListener = 
        new OnBackStackChangedListener(){

	public void onBackStackChanged(){
	    FragmentManager manager = getFragmentManager();
	    if (manager != null){
		int backStackSize = manager.getBackStackEntryCount();
		if(backStackSize != 0){
		    FragmentManager.BackStackEntry backEntry = null;
		    backEntry = manager.getBackStackEntryAt(backStackSize - 1);
		    getActionBar().setTitle(backEntry.getName());
		}else{
		    // Default to launcher since this is the first fragment
		    finish();
		}
	    }
	}
	
    };
	
    // The click listener for ListView in the navigation drawer
    private ListView.OnItemClickListener mDrawerClickListener = 
        new ListView.OnItemClickListener(){
	@Override
        public void onItemClick(
	    AdapterView<?> parent, View view, final int position, long id
	){
            mDrawerLayout.setDrawerListener(
		new DrawerLayout.SimpleDrawerListener(){
		    @Override
		    public void onDrawerClosed(View drawerView){
			super.onDrawerClosed(drawerView);
			getActionBar().setDisplayShowTitleEnabled(true);
			
			FragmentTransaction fragmentTx = getFragmentManager(
			).beginTransaction();
			
			String fragmentTitle = mFragmentTitles[position];
			if(fragmentTitle.equals("Navigation")){
			    Intent intent = new Intent();
			    intent.setClassName(
				"com.google.android.apps.maps",
				"com.google.android.maps.MapsActivity"
			    ); 
			    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			    startActivity(intent);
			}else{
			    fragmentTx.replace(
				R.id.main, mFragments[position]
			    );
			    fragmentTx.addToBackStack(fragmentTitle);
			    fragmentTx.commit();
			}
		    }
		}
	    );
            mDrawerList.clearChoices();
            mDrawerListAdapter.notifyDataSetChanged();
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    };
    
    //  Options selected updater
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                if(mDrawerLayout.isDrawerOpen(mDrawerList)){
                    mDrawerLayout.closeDrawer(mDrawerList);
                }
                else {
                    mDrawerLayout.openDrawer(mDrawerList);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    // Android specific overrides
	
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
	
        // Load Drawer
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        
        String[] settingsItems = getResources().getStringArray(
	    R.array.options_array
	);
        
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerToggle = new ActionBarDrawerToggle(
            this, mDrawerLayout, R.drawable.ic_drawer,
	    R.string.drawer_open, R.string.drawer_close
        ){
	    // Called when a drawer has settled in a completely closed state.
	    public void onDrawerClosed(View view){
		super.onDrawerClosed(view);
		invalidateOptionsMenu();
	    }
	    // Called when a drawer has settled in a completely open state.
	    public void onDrawerOpened(View drawerView){
		super.onDrawerOpened(drawerView);
		invalidateOptionsMenu();
	    }

        };
	
        mDrawerLayout.post(new Runnable(){
	    
            @Override
            public void run(){
                mDrawerToggle.syncState();
            }

        });

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getActionBar().setIcon(R.drawable.ic_drawer);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Set up the drawer's list view with items and click listener
        mDrawerListAdapter = new ArrayAdapter<String>(
            this, R.layout.drawer_item, settingsItems
        );
        mDrawerList.setAdapter(mDrawerListAdapter);
        mDrawerList.setOnItemClickListener(mDrawerClickListener);
	
        if(savedInstanceState == null){
            getFragmentManager().addOnBackStackChangedListener(
		mBackstackStateListener
	    );
            // Add the main fragment.
            FragmentTransaction fragmentTx = getFragmentManager(
	    ).beginTransaction();
            fragmentTx.replace(R.id.main, mFragments[0]);
            getActionBar().setDisplayShowTitleEnabled(true);
            fragmentTx.addToBackStack(mFragmentTitles[0]);
            fragmentTx.commit();
        }
    }

}