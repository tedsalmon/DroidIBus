package net.littlebigisland.droidibus.activity;
/**
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 *
 */
import net.littlebigisland.droidibus.R;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends FragmentActivity {
	private DrawerLayout mDrawerLayout = null;
	private ListView mDrawerList = null;
	
	private String[] mFragments = {
		"net.littlebigisland.droidibus.activity.DashboardFragment",
		"net.littlebigisland.droidibus.activity.NavigationFragment",
		"net.littlebigisland.droidibus.activity.SettingsFragment",
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
		 setContentView(R.layout.main_activity);
		 
		// Take care of the drawer
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		
		String[] mSettingsItems = getResources().getStringArray(R.array.options_array);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		
		// set up the drawer's list view with items and click listener
		mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_item, mSettingsItems));
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		FragmentTransaction fragmentTx = getFragmentManager().beginTransaction();
		fragmentTx.replace(
			R.id.main,
			Fragment.instantiate(
				MainActivity.this,
				mFragments[0]
			)
		);
		fragmentTx.commit();
	}
	
    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        	mDrawerLayout.setDrawerListener( new DrawerLayout.SimpleDrawerListener(){
        		@Override
        		public void onDrawerClosed(View drawerView){
        			super.onDrawerClosed(drawerView);
        			FragmentTransaction fragmentTx = getFragmentManager().beginTransaction();
        			fragmentTx.replace(
        				R.id.main,
        				Fragment.instantiate(
        					MainActivity.this, mFragments[position]
        				)
        			);
        			fragmentTx.commit();
        		}
			});
        	mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

}