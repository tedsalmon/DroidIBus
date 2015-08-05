package net.littlebigisland.droidibus.resources;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

public class ServiceManager{
    
    
    @SuppressWarnings("rawtypes")
    public static boolean startService(Class cls, ServiceConnection svcConn, 
            Context context){
        Intent svcIntent = new Intent(context, cls);
        try{
            context.bindService(svcIntent, svcConn, Context.BIND_AUTO_CREATE);
            context.startService(svcIntent);
        }catch(Exception ex){
            return false;
        }
        return true;
    }
    
    @SuppressWarnings("rawtypes")
    public static boolean stopService(Class cls, ServiceConnection svcConn, 
            Context context){
        try{
            context.unbindService(svcConn);
        }catch(Exception ex){
            return false;
        }
        return true;
    }
}
