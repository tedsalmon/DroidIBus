package net.littlebigisland.droidibus.activity;

import net.littlebigisland.droidibus.R;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingsFragment extends PreferenceFragment{
	public String TAG = "DroidIBus";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.settings, container, false);
		addPreferencesFromResource(R.xml.settings_data);
		return v;
	}
}