package net.littlebigisland.droidibus.activity;
/**
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 *
 */
import net.littlebigisland.droidibus.R;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends FragmentActivity {
	/**
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DashboardFragment fragment;
		if(savedInstanceState == null) {
			fragment = new DashboardFragment();
			getSupportFragmentManager()
				.beginTransaction()
				.add(android.R.id.content, fragment)
				.commit();
		}
		else {
			fragment = new DashboardFragment();
		}
	}*/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
		 setContentView(R.layout.main_activity);
		 
		// Take care of the drawer
		ListView mDrawerList = (ListView) findViewById(R.id.left_drawer);
		
		String[] mSettingsItems = getResources().getStringArray(R.array.options_array);
		
		//DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		
		// set up the drawer's list view with items and click listener
		mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_item, mSettingsItems));
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
		tx.replace(
			R.id.main, 
			Fragment.instantiate(
				MainActivity.this,
				"net.littlebigisland.droidibus.activity.DashboardFragment"
			)
		);
		tx.commit();
	}
	
    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        	//getActivity().getC.selectItem(position);
        	/**
        	 @Override
        	 drawer.closeDrawer(navList);
			 public void onItemClick(AdapterView<?> parent, View view, final int pos,long id){
				 drawer.setDrawerListener( new DrawerLayout.SimpleDrawerListener(){
					 @Override
					 public void onDrawerClosed(View drawerView){
						 super.onDrawerClosed(drawerView);
						 FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
						 tx.replace(R.id.main, Fragment.instantiate(MainActivity.this, fragments[pos]));
						 tx.commit();
					 }
				});
				 drawer.closeDrawer(navList);
        	 */
        }
    }

}