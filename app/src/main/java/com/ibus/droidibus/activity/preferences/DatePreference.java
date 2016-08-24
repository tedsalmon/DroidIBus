package com.ibus.droidibus.activity.preferences;

import java.util.Calendar;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;

public class DatePreference extends DialogPreference{
    
    private DatePicker mDatePicker = null;
    private String mCurrentDate = null;
    
    public DatePreference(Context ctxt) {
        this(ctxt, null);
    }

    public DatePreference(Context ctxt, AttributeSet attrs){
        this(ctxt, attrs, 0);
    }

    public DatePreference(Context ctxt, AttributeSet attrs, int defStyle){
        super(ctxt, attrs, defStyle);
        setPositiveButtonText("Set");
        setNegativeButtonText("Cancel");
    }
    
    @Override
    protected void onBindDialogView(View v){
        super.onBindDialogView(v);
        if(mCurrentDate != null){
            String[] tDate = mCurrentDate.split("-");
            mDatePicker.updateDate(
                Integer.parseInt(tDate[0]),
                Integer.parseInt(tDate[1]) - 1,
                Integer.parseInt(tDate[2])
            );
        }else{
            // Default to the tablets date
            Calendar tempDate = Calendar.getInstance();
            mDatePicker.updateDate(
                tempDate.get(Calendar.YEAR),
                tempDate.get(Calendar.MONTH),
                tempDate.get(Calendar.DATE)
            );
        }
    }
    
    @Override
    protected View onCreateDialogView(){
        mDatePicker = new DatePicker(getContext());
        return mDatePicker;
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult){
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
    
    @Override
    public void setDefaultValue(Object value){
        mCurrentDate = (String) value;
    }

}
