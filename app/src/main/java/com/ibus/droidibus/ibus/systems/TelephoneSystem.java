package com.ibus.droidibus.ibus.systems;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;

import com.ibus.droidibus.ibus.IBusSystem;

public class TelephoneSystem extends IBusSystem{

    /**
     * Messages from the Navigation System to the Telephone
     */
    class NavigationSystem extends IBusSystem{
        
        private byte locationData = (byte) 0xA4;
        private byte gpsData = (byte) 0xA2;
        
        @SuppressLint("UseSparseArrays") 
        private Map<Byte, Method> IBusNavigationMap = new HashMap<Byte, Method>();
        
        private String bcdToIntString(String value){
            return String.format(Locale.US, "%02d", Integer.parseInt(value));
        }
        
        private double decimalMinsToDecimalDeg(String deg, String mins, String secs){
            double degrees = Double.parseDouble(deg);
            double minutes = Double.parseDouble(mins);
            double seconds = Double.parseDouble(secs);
            
            double minSecsInDecimal = (((minutes * 60) + seconds) / 3600);
            
            return degrees+minSecsInDecimal;
            
        }
        
        public void parseGPSData(){
            // Parse out coordinate data
            List<String> coordData = new ArrayList<String>();
            for (int i = 6; i < currentMessage.size(); i++){
                // Convert to int to rid ourselves of preceding zeros. Yes, this is bad.
                coordData.add(
                    String.valueOf(
                        Integer.parseInt(
                            bcdToStr(currentMessage.get(i))
                        )
                    )
                );
            }

            String latitudeNorthSouth = (
                bcdToIntString(coordData.get(3)).charAt(1) == '0'
            ) ? "N" : "S";
            
            String longitudeEastWest = (
                bcdToIntString(coordData.get(8)).charAt(1) == '0'
            ) ? "E" : "W";
            
            triggerCallback("onUpdateGPSCoordinates",
                String.format(
                    "%.4f%s%s %.4f%s%s", 
                    //Latitude
                    decimalMinsToDecimalDeg(
                        coordData.get(0), coordData.get(1), coordData.get(2)
                    ),
                    (char) 0x00B0,
                    latitudeNorthSouth,
                    //Longitude
                    decimalMinsToDecimalDeg(
                        coordData.get(4) + coordData.get(5),
                        coordData.get(6),
                        coordData.get(7)
                    ),
                    (char) 0x00B0,
                    longitudeEastWest
                )
            );
        
            // Parse out altitude data which is in meters and is stored as a byte coded decimal
            int altitude = Integer.parseInt(
                bcdToStr(currentMessage.get(15)) + bcdToStr(currentMessage.get(16))
            );

            triggerCallback("onUpdateGPSAltitude", altitude);
            
            // Parse out time data which is in UTC
            String gpsUTCTime = String.format(
                "%s:%s",
                bcdToIntString(bcdToStr(currentMessage.get(18))),
                bcdToIntString(bcdToStr(currentMessage.get(19)))
            );
            
            triggerCallback("onUpdateGPSTime", gpsUTCTime);
        }
                
        public void setLocale(){
            int lastData = 6;
            while(currentMessage.get(lastData) != (byte) 0x00){
                lastData++;
            }
            triggerCallback("onUpdateLocale", decodeMessage(currentMessage, 6, lastData-1));
        }
        
        public void setStreetLocation(){
            int lastData = 6;
            while(currentMessage.get(lastData) != (byte)0x3B){
                lastData++;
            }
            triggerCallback("onUpdateStreetLocation", decodeMessage(currentMessage, 6, lastData-1));
        }
        
        NavigationSystem(){
            try{
                IBusNavigationMap.put((byte)0x00, this.getClass().getMethod("parseGPSData"));
                IBusNavigationMap.put((byte)0x01, this.getClass().getMethod("setLocale"));
                IBusNavigationMap.put((byte)0x02, this.getClass().getMethod("setStreetLocation"));
            }catch(NoSuchMethodException e){
                // First world anarchy
            }
        }
        
        public void mapReceived(ArrayList<Byte> msg){
            currentMessage = msg;
            try{
                if(msg.get(3) == locationData || msg.get(3) == gpsData){
                    IBusNavigationMap.get(msg.get(5)).invoke(this);
                }
            }catch(IllegalArgumentException | InvocationTargetException | IllegalAccessException e){
                e.printStackTrace();
            }
        }
        
    }
    
    /**
     * Message from the MFL to the Telephone
     */
    class SteeringWheelSystem extends IBusSystem{

        public void mapReceived(ArrayList<Byte> msg) {
            currentMessage = msg;
            if(currentMessage.get(3) == 0x3B){
                if(currentMessage.get(4) == (byte) 0xA0){ // Voice Btn
                    triggerCallback("onVoiceBtnPress");
                }
                if(currentMessage.get(4) == (byte) 0x90){
                    triggerCallback("onVoiceBtnHold");
                }
                
            }
        }
        
    }
    
    public TelephoneSystem(){
        IBusDestinationSystems.put(
            Devices.MultiFunctionSteeringWheel.toByte(),
            new SteeringWheelSystem()
        );
        IBusDestinationSystems.put(
            Devices.NavigationEurope.toByte(), new NavigationSystem()
        );
    }
    
}
