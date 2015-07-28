package net.littlebigisland.droidibus.ibus;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;

/**
 * This class represents the base of which all IBus Systems will extend from
 * in order to parse sent/received messages.
 * 
 * Subsequent subclasses implement message handlers for destination systems
 * using this same class. I.e. Source system IKE Extends IBusSystem
 * it's child class, say GlobalBroadcastSystem also implements IBusSystem.
 * This represents an IBus Message's "Source" and "Destination" system architecture.
 * 
 * Please note that it makes the most sense to do received message processing inside the
 * destination implementation HOWEVER, the source system should be the one handling
 * out bound messages to another destination system. For example, BM -> IKE Messages
 * should be initiated from the BM Class, not the IKE class nor a child
 * implementation of the IKE class inside the BM class.
 */
public class IBusSystem{
    
    private String TAG = "DroidIBus";
    
    // Hold a map of what instances have requested callbacks
    private Map<String, CallbackHolder> mRegisteredCallbacks = new HashMap<String, CallbackHolder>();
    
    // ArrayList holding the message currently being processed.
    public ArrayList<Byte> currentMessage = null;
    
    // Map used to map implementation of Destination systems from each Source System 
    @SuppressLint("UseSparseArrays")
    public Map<Byte, IBusSystem> IBusDestinationSystems = new HashMap<Byte, IBusSystem>();
    
    /**
     * Abstract class for all defined callbacks
     * TODO Move this to the message service instead?
     */
    public static abstract class Callbacks{
    
        // Radio System
        public void onUpdateRadioStation(final String text){}
        
        public void onUpdateRadioBrodcasts(final String broadcastType){}
        
        public void onUpdateRadioStereoIndicator(final String stereoIndicator){}
        
        public void onUpdateRadioRDSIndicator(final String rdsIndicator){}
        
        public void onUpdateRadioProgramIndicator(final String currentProgram){}
        
        public void onUpdateRadioStatus(final int status){}
        
        public void onRadioCDStatusRequest(){}
        
        // IKE System
        public void onUpdateRange(final String range){}
    
        public void onUpdateOutdoorTemp(final String temp){}
    
        public void onUpdateFuel1(final String mpg){}
    
        public void onUpdateFuel2(final String mpg){}
    
        public void onUpdateAvgSpeed(final String speed){}
        
        public void onUpdateTime(final String time){}
        
        public void onUpdateDate(final String date){}
        
        public void onUpdateSpeed(final int speed){}
    
        public void onUpdateRPM(final int rpm){}
        
        public void onUpdateCoolantTemp(final int temp){}
        
        public void onUpdateIgnitionSate(final int state){}
        
        public void onUpdateUnits(final String units){}
        
        // Navigation System
        public void onUpdateStreetLocation(final String streetName){}
        
        public void onUpdateGPSAltitude(final int altitude) {}
        
        public void onUpdateGPSCoordinates(final String gpsCoordinates){}
        
        public void onUpdateGPSTime(final String time){}
        
        public void onUpdateLocale(final String cityName){}
        
        // Steering Wheel System
        public void onTrackFwd(){}
        
        public void onTrackPrev(){}
        
        public void onVoiceBtnPress(){}
        
        public void onVoiceBtnHold(){}
        
        // Telephone System
        public void onUpdateIKEDisplay(final String text){}
        
        // Light Control System
        public void onLightStatus(final int lightStatus){}
    }
    
    // Simple class to hold our Receiver and Handler for each activity registered for callbacks
    private class CallbackHolder{
        
        private Callbacks mIBusReceiver = null;
        private Handler mActivityHandler = null;
        
        public CallbackHolder(Callbacks mIBusRecvr, Handler activityHandler){
            mIBusReceiver = mIBusRecvr;
            mActivityHandler = activityHandler;
        }
        
        public Callbacks getReceiver(){
            return mIBusReceiver;
        }
        
        public Handler getHandler(){
            return mActivityHandler;
        }
    }
    
    /**
     * The address off all the systems linked via IBus
     */
    public enum Devices{
        // System constants
        BodyModule((byte) 0x00),
        SunroofControl((byte) 0x08),
        CDChanger((byte) 0x18),
        RadioControlledClock((byte) 0x28),
        CheckControlModule((byte) 0x30),
        GFXNavigationDriver((byte) 0x3B),
        Diagnostic((byte) 0x3F),
        RemoteControlCentralLocking((byte) 0x40),
        GFXDriverRearScreen((byte) 0x43),
        Immobiliser((byte) 0x44),
        CentralInformationDisplay((byte) 0x46),
        MultiFunctionSteeringWheel((byte) 0x50),
        MirrorMemory((byte) 0x51),
        IntegratedHeatingAndAirConditioning((byte) 0x5B),
        ParkDistanceControl((byte) 0x60),
        Radio((byte) 0x68),
        DigitalSignalProcessingAudioAmplifier((byte) 0x6A),
        SeatMemory((byte) 0x72),
        SiriusRadio((byte) 0x73),
        CDChangerDINsize((byte) 0x76),
        NavigationEurope((byte) 0x7F),
        InstrumentClusterElectronics((byte) 0x80),
        MirrorMemorySecond((byte) 0x9B),
        MirrorMemoryThird((byte) 0x9C),
        RearMultiInfoDisplay((byte) 0xA0),
        AirBagModule((byte) 0xA4),
        SpeedRecognitionSystem((byte) 0xB0),
        NavigationJapan((byte) 0xBB),
        GlobalBroadcast((byte) 0xBF),
        MultiInfoDisplay((byte) 0xC0),
        Telephone((byte) 0xC8),
        Assist((byte) 0xCA),
        LightControlModule((byte) 0xD0),
        SeatMemorySecond((byte) 0xDA),
        IntegratedRadioInformationSystem((byte) 0xE0),
        FrontDisplay((byte) 0xE7),
        RainLightSensor((byte) 0xE8),
        Television((byte) 0xED),
        BoardMonitor((byte) 0xF0),
        CSU((byte) 0xF5),
        Broadcast((byte) 0xFF),
        Unset((byte) 0x100),
        Unknown((byte) 0x101);
        
        private final byte value;
        
        Devices(byte value) {
            this.value = value;
        }

        public byte toByte(){
            return value;
        }
        
    }
    
    /** 
     * Converts a Byte Coded Decimal to it's String representation
     * @param bcd The byte to decode
     * @return       The String representation of the param byte
     */
    public String bcdToStr(byte bcd) {
        StringBuffer strBuff = new StringBuffer();
        
        byte high = (byte) (bcd & 0xf0);
        high >>>= (byte) 4;    
        high = (byte) (high & 0x0f);
        byte low = (byte) (bcd & 0x0f);
        
        strBuff.append(high);
        strBuff.append(low);
        
        return strBuff.toString();
    }
    
    /** 
     * Decode Parts of an IBus Message from Bytes into a readable String
     * 
     * @param msg         The Message we're decoding
     * @param startByte    The index of the first byte we are extracting
     * @param endByte    The index of the last byte we are extracting
     * @return The message in UTF-8 String formatting
     */
    public String decodeMessage(ArrayList<Byte> msg, int startByte, int endByte){
        ArrayList<Byte> tempBytes = new ArrayList<Byte>();
        while(startByte <= endByte){
            tempBytes.add(msg.get(startByte));
            startByte++;
        }
        byte[] strByte = new byte[tempBytes.size()];
        for(int i = 0; i < tempBytes.size(); i++){
            strByte[i] = tempBytes.get(i);
        }
        try {
            return new String(strByte, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * Generate CRC for the message
     */
    public byte genMessageCRC(byte[] msg){
        byte crc = 0x00;
        for(int i = 0; i < msg.length; i++)
            crc = (byte) (crc ^ msg[i]);
        return crc;
    }
    
    /** 
     * Use the implementations destination system Map to lookup
     * the instance class that handles the given message. In some instances
     * it makes to override this method with more implementation specific code.
     * 
     * @param msg    The Message to be handled
     * @throws NoSuchMethodException 
     */
    public void mapReceived(ArrayList<Byte> msg){
        IBusSystem targetSystem = IBusDestinationSystems.get((byte) msg.get(0));
        if(targetSystem != null){
            targetSystem.mapReceived(msg);
        }
    }
    
    /**
     * Register the programmers callback implementation with the class.
     * Introspectively register this receiver to all children in the
     * `IBusDestinationSystems` Map.
     * 
     * @param cb     Your implementation of the IBusMessageReceiver
     * 
     */
    public void registerCallback(Callbacks cb, Handler handler){
        mRegisteredCallbacks.put(cb.toString(), new CallbackHolder(cb, handler));
        for (Object key : IBusDestinationSystems.keySet()){
            IBusDestinationSystems.get(key).registerCallback(cb, handler);
        }
    }
    
    public void unregisterCallback(Callbacks cb){
        mRegisteredCallbacks.remove(cb.toString());
    }
    
    /**
     * Trigger a method callback. Check to see if the callback receiver has been defined, 
     * if so call the given method.
     * @param callback The name of the function to trigger
     * @throws NoSuchMethodException 
     */
    public void triggerCallback(final String callback){
        for (String key : mRegisteredCallbacks.keySet()){
            final CallbackHolder tempCallback = mRegisteredCallbacks.get(key);
            final Handler mHandler = tempCallback.getHandler();
            final Callbacks mCallbackReceiver = tempCallback.getReceiver();
            mHandler.post(new Runnable(){
                @Override
                public void run() {
                    try{
                        Log.d(TAG, String.format("Triggering '%s()'", callback.toString()));
                        Method cb = mCallbackReceiver.getClass().getMethod(callback);
                        cb.invoke(mCallbackReceiver);
                    }catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }
    
    /**
     * Trigger a method callback. Check to see if the callback receiver has been defined, 
     * if so call the given method with the given parameters (if any). Overloaded 
     * for different data types.
     * @param callback The name of the function to trigger
     * @param value    The string value to pass to the callback
     */
    public void triggerCallback(final String callback, final String value){
        for (String key : mRegisteredCallbacks.keySet()){
            final CallbackHolder tempCallback = mRegisteredCallbacks.get(key);
            final Handler mHandler = tempCallback.getHandler();
            final Callbacks mCallbackReceiver = tempCallback.getReceiver();
            mHandler.post(new Runnable(){
                @Override
                public void run() {
                    try{
                        Log.d(TAG, String.format("Triggering '%s()' with value '%s'", callback.toString(), value));
                        Method cb = mCallbackReceiver.getClass().getMethod(callback, String.class);
                        cb.invoke(mCallbackReceiver, value);
                    }catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }
    
    /**
     * Trigger a method callback. Check to see if the callback receiver has been defined, 
     * if so call the given method with the given parameters (if any). Overloaded 
     * for different data types.
     * @param callback The name of the function to trigger
     * @param value    The integer value to pass to the callback
     */    
    public void triggerCallback(final String callback, final int value){
        for (String key : mRegisteredCallbacks.keySet()){
            final CallbackHolder tempCallback = mRegisteredCallbacks.get(key);
            final Handler mHandler = tempCallback.getHandler();
            final Callbacks mCallbackReceiver = tempCallback.getReceiver();
            mHandler.post(new Runnable(){
                @Override
                public void run() {
                    try{
                        Log.d(TAG, String.format("Triggering '%s()' with value '%s'", callback.toString(), value));
                        Method cb = mCallbackReceiver.getClass().getMethod(callback, int.class);
                        cb.invoke(mCallbackReceiver, value);
                    }catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
