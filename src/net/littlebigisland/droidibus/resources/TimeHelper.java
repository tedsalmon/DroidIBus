package net.littlebigisland.droidibus.resources;

import java.util.Calendar;

public class TimeHelper{
    
    public static long getTimeNow(){
        return Calendar.getInstance().getTimeInMillis();
    }
}
