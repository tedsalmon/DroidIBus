package net.littlebigisland.droidibus.activity.preferences;

import java.util.Locale;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

public class TimePreference extends DialogPreference{
    
    private int lastHour = 0; // Default to 00:00
    private int lastMinute = 0;
    private TimePicker picker = null;

    public static int getHour(String time){
        String[] pieces = time.split(":");
        return Integer.parseInt(pieces[0]);
    }

    public static int getMinute(String time){
        String[] pieces = time.split(":");
        return Integer.parseInt(pieces[1]);
    }

    public TimePreference(Context ctxt){
        this(ctxt, null);
    }

    public TimePreference(Context ctxt, AttributeSet attrs){
        this(ctxt, attrs, 0);
    }

    public TimePreference(Context ctxt, AttributeSet attrs, int defStyle){
        super(ctxt, attrs, defStyle);
        setPositiveButtonText("Set");
        setNegativeButtonText("Cancel");
    }

    @Override
    protected View onCreateDialogView(){
        picker = new TimePicker(getContext());
        picker.setIs24HourView(true);
        return picker;
    }

    @Override
    protected void onBindDialogView(View v){
        super.onBindDialogView(v);
        picker.setCurrentHour(lastHour);
        picker.setCurrentMinute(lastMinute);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult){
        super.onDialogClosed(positiveResult);
        if(positiveResult){
            lastHour = picker.getCurrentHour();
            lastMinute = picker.getCurrentMinute();
            String currentTime = String.format(
                Locale.US, "%02d:%02d", lastHour, lastMinute
            );
            if(callChangeListener(currentTime)){
                persistString(currentTime);
            }
        }
    }
    
    @Override
    public void setDefaultValue(Object value){
        lastHour = getHour((String) value);
        lastMinute = getMinute((String) value);
    }

}