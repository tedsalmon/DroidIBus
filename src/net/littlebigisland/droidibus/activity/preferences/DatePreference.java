package net.littlebigisland.droidibus.activity.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;

public class DatePreference extends DialogPreference{
	private DatePicker mDatePicker = null;
	private String mCurrentDate = "";
	
	public DatePreference(Context ctxt) {
        this(ctxt, null);
    }

    public DatePreference(Context ctxt, AttributeSet attrs) {
        this(ctxt, attrs, 0);
    }

    public DatePreference(Context ctxt, AttributeSet attrs, int defStyle) {
        super(ctxt, attrs, defStyle);
        setPositiveButtonText("Set");
        setNegativeButtonText("Cancel");
    }
    
    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mDatePicker.updateDate(2014, 07, 25);
    }
    
    @Override
    protected View onCreateDialogView() {
    	mDatePicker = new DatePicker(getContext());
        return(mDatePicker);
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
        	mCurrentDate = String.format(
        		"%s %s %s",
        		mDatePicker.getDayOfMonth(),
        		mDatePicker.getMonth() + 1,
        		mDatePicker.getYear() - 2000 // BEWARE POTENTIAL BUGS
        	);
            if (callChangeListener(mCurrentDate)) {
                persistString(mCurrentDate);
            }
        }
    }
}
