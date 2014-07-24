package net.littlebigisland.droidibus.activity.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class SettingsCarDatePreference extends DialogPreference{
    
	public SettingsCarDatePreference(Context ctxt) {
        this(ctxt, null);
    }

    public SettingsCarDatePreference(Context ctxt, AttributeSet attrs) {
        this(ctxt, attrs, 0);
    }

    public SettingsCarDatePreference(Context ctxt, AttributeSet attrs, int defStyle) {
        super(ctxt, attrs, defStyle);
        setPositiveButtonText("Set");
        setNegativeButtonText("Cancel");
    }
}
