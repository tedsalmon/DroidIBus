package net.littlebigisland.droidibus;
/**
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 *
 */
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class MainActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MainControlFragment fragment;
		if(savedInstanceState == null) {
			fragment = new MainControlFragment();
			getSupportFragmentManager()
				.beginTransaction()
				.add(android.R.id.content, fragment)
				.commit();
		}
		else {
			fragment = new MainControlFragment();
		}
	}
}