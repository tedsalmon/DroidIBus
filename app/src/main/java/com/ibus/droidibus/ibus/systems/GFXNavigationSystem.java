package com.ibus.droidibus.ibus.systems;
import java.util.ArrayList;
import java.util.Locale;

import com.ibus.droidibus.ibus.IBusSystem;

public class GFXNavigationSystem extends IBusSystem{
    
    // Main Systems
    private static final byte GFX_DRIVER = Devices.GFXNavigationDriver.toByte();
    private static final byte IKE_SYSTEM = Devices.InstrumentClusterElectronics.toByte();
    
    // OBC Functions
    private byte OBCRequest = 0x41;
    private byte OBCRequestGet = 0x01;
    private byte OBCRequestReset = 0x10;
    private byte OBCRequestSet = 0x40;
    private byte OBCUnitSet = 0x15;
    
    
    private static final byte[] BASS_LEVELS = new byte[]{
        (byte) 0x9C, (byte) 0x9A, (byte) 0x98, (byte) 0x96, (byte) 0x94,
        (byte) 0x92, (byte) 0x90, (byte) 0x82, (byte) 0x84, (byte) 0x86,
        (byte) 0x88, (byte) 0x8A, (byte) 0x8C
    };
    
    private static final byte[] TREB_LEVELS = new byte[]{
        0x1C, 0x1A, 0x18, 0x16, 0x14, 0x12, 0x10, 0x02, 0x04, 0x06,0x08, 0x0A, 0x0C
    };
    
    private static final byte[] FADE_BAL_LEVELS = new byte[]{
        0x1F, 0x1E, 0x1C, 0x1A, 0x18, 0x16, 0x14, 0x12, 0x11, 0x10, 0x01, 
        0x02, 0x03, 0x04, 0x05, 0x06, 0x08, 0x0A, 0x0C, 0x0E, 0x0F
    };
    
    private enum ToneType{
        BASS, TREB, FADE, BAL
    }
    
    /**
     *  Messages from Radio in the trunk to the BoardMonitor
     */
    class RadioSystem extends IBusSystem{
        
        private static final byte INFO_MENU = 0x21;
        private static final byte RDS_TEXT = 0x23;
        private static final byte TONE_DATA = 0x37;
        private static final byte SCREEN_UPDATE = 0x46;
        private static final byte RADIO_METADATA = (byte) 0xA5;
        
        private static final byte RDS_VAL = (byte) 0x82;
        private static final byte PTY_VAL = 0x41;

        public void mapReceived(ArrayList<Byte> msg){
            currentMessage = msg;
            
            switch(currentMessage.get(3)){
                case INFO_MENU:
                    switch(currentMessage.get(6)){
                        case RDS_VAL:
                            int rds = (currentMessage.get(20) == 0x2A) ? 1: 0;
                            triggerCallback("onUpdateRDSStatus", rds);
                            break;
                        case PTY_VAL:
                            int pty = (currentMessage.get(20) == 0x2A) ? 1: 0;
                            triggerCallback("onUpdatePTYStatus", pty);
                            break;
                    }
                    break;
                case RDS_TEXT:
                    triggerCallback("onUpdateRadioStation", decodeData());
                    break;
                case SCREEN_UPDATE:
                    /* Screen modes:
                     68 04 3B 46 01 10 -> Gracefully returned to BMBT
                     68 04 3B 46 02 13 -> Move to BMBT Screen - Timeout
                     68 04 3B 46 04 15 -> Move to RDS from SEL - Timeout
                     68 04 3B 46 08 19 -> Move to RDS from Tone
                     68 04 3B 46 0C 1D -> Clear middle area / Seen every time other messages are sent
                     */
                    byte screen_mode = currentMessage.get(4);
                    triggerCallback("onUpdateScreenState", (int) screen_mode);
                    break;
                case TONE_DATA:
                    // Check to make sure we're getting the right data
                    if(currentMessage.get(1) == 0x07){
                        int bass = getToneLevel(
                            ToneType.BASS, currentMessage.get(4)
                        );
                        int treb = getToneLevel(
                            ToneType.TREB, currentMessage.get(5)
                        );
                        int fade = getToneLevel(
                            ToneType.FADE, currentMessage.get(6)
                        );
                        int bal = getToneLevel(
                            ToneType.BAL, currentMessage.get(7)
                        );
                        triggerCallback(
                            "onUpdateToneLevels", bass, treb, fade, bal
                        );
                    }
                    break;
                case RADIO_METADATA:
                    byte metaDataType = currentMessage.get(6);
                    String dataText = decodeData();
                    switch(metaDataType){
                        case 0x41: // Broadcast Type
                            triggerCallback("onUpdateRadioBrodcasts", dataText);
                            break;
                        case 0x04:
                        case 0x44: // Stereo Indicators
                            triggerCallback(
                                "onUpdateRadioStereoIndicator", dataText
                            );
                            break;
                        case 0x45: // Radio Data System Indicator
                            triggerCallback(
                                "onUpdateRadioRDSIndicator", dataText
                            );
                            break;
                        case 0x02: // Program Type
                            triggerCallback(
                                "onUpdateRadioProgramIndicator", dataText
                            );
                            break;
                    }
                    break;
            }

        }

        /**
         * Iterate through the message and remove all non-ASCII data
         * 
         * @return String     String representation of data
         */
        private String decodeData(){
            // Radio RDS starts 6 bytes in, metadata at 7
            int startByte = (currentMessage.get(3) == RDS_TEXT) ? 6 : 7;
            // Remove control characters - Anything under 0x20 matches
            for(int i = startByte; i < currentMessage.size() - 2; i++){
                if(currentMessage.get(i) < (byte)0x20){
                    currentMessage.remove(i);
                }
            }
            // Skip the CRC
            int endByte = currentMessage.size() - 2;
            
            // Remove the padding from the front and back of the message
            while(currentMessage.get(endByte) == 0x20){
                endByte--;
            }
            while(currentMessage.get(startByte) == 0x20){
                startByte++;
            }
            return decodeMessage(currentMessage, startByte, endByte);            
        }
        
        /**
         * 
         */
         private int getToneLevel(ToneType type, byte val){
             int levelIndex = 0;
             switch(type){
                 case BASS:
                     levelIndex = getIndex(BASS_LEVELS, val);
                     break;
                 case TREB:
                     levelIndex = getIndex(TREB_LEVELS, val);
                     break;
                 case FADE:
                     levelIndex = getIndex(FADE_BAL_LEVELS, val);
                     break;
                 case BAL:
                     levelIndex = getIndex(FADE_BAL_LEVELS, val) - 10;
                     break;
             }
             return levelIndex;
         }
         
         private int getIndex(byte[] bytes, byte val){
             for(int i = 0; i < bytes.length; i++){
                 if(bytes[i] == val){
                     return i;
                 }
             }
             return -1;
         }

    }
    
    /**
     * Generate an IKE message requesting a value reset for the given system.
     * @param system The hex value of the system in question
     * @param checksum The hex value of the message checksum
     * @return Byte array of message to send to IBus
     */
    private byte[] IKEGetRequest(int system, int checksum){
        return new byte[] {
            GFX_DRIVER, 0x05, IKE_SYSTEM, 
            OBCRequest, (byte)system, OBCRequestGet, (byte)checksum
        };
    }
    
    /**
     * Generate an IKE message requesting the value for the given system.
     * @param system The hex value of the system in question
     * @param checksum The hex value of the message checksum
     * @return Byte array of message to send to IBus
     */
    private byte[] IKEResetRequest(int system, int checksum){
        return new byte[] {
            GFX_DRIVER, 0x05, IKE_SYSTEM, 
            OBCRequest, (byte)system, OBCRequestReset, (byte)checksum 
        };
    }
    
    /**
     * Issue a Get request for the "Time" Value.
     * @return Byte array of message to send to IBus
     */
    public byte[] getTime(){
        return IKEGetRequest(0x01, 0xFF);
    }
    
    /**
     * Issue a Get request for the "Date" Value.
     * @return Byte array of message to send to IBus
     */
    public byte[] getDate(){
        return IKEGetRequest(0x02, 0xFC);
    }
    
    /**
     * Issue a Get request for the "Outdoor Temp" Value.
     * @return Byte array of message to send to IBus
     */
    public byte[] getOutdoorTemp(){
        return IKEGetRequest(0x03, 0xFD);
    }
    
    /**
     * Issue a Get request for the "Consumption 1" Value.
     * @return Byte array of message to send to IBus
     */
    public byte[] getFuel1(){
        return IKEGetRequest(0x04, 0xFA);
    }
    
    /**
     * Issue a Get request for the "Consumption 2" Value.
     * @return Byte array of message to send to IBus
     */
    public byte[] getFuel2(){
        return IKEGetRequest(0x05, 0xFB);
    }
    
    /**
     * Issue a Get request for the "Fuel Tank Range" Value.
     * @return Byte array of message to send to IBus
     */
    public byte[] getRange(){
        return IKEGetRequest(0x06, 0xF8);
    }
    
    /**
     * Issue a Get request for the "Avg. Speed" Value.
     * @return Byte array of message to send to IBus
     */
    public byte[] getAvgSpeed(){
        return IKEGetRequest(0x0A, 0xF4);
    }
    
    /**
     * Reset the "Consumption 1" IKE metric
     * @return Byte array of message to send to IBus
     */
    public byte[] resetFuel1(){
        return IKEResetRequest(0x04, 0xEB);
    }
    
    /**
     * Reset the "Consumption 2" IKE metric
     * @return Byte array of message to send to IBus
     */
    public byte[] resetFuel2(){
        return IKEResetRequest(0x05, 0xEA);
    }
    
    /**
     * Reset the "Avg. Speed" IKE metric
     * @return Byte array of message to send to IBus
     */
    public byte[] resetAvgSpeed(){
        return IKEResetRequest(0x0A, 0xE5);
    }
    
    /**
     * Send a new time setting to the IKE
     * IBus message: 3B 06 80 40 01 <Hours> <Mins> <CRC>
     * @param args Two ints MUST be provided
     *  int hours, int minutes
     * @return Byte array of composed message to send to IBus
     */
    public byte[] setTime(Object... args){
        int hours = (Integer) args[0];
        int minutes = (Integer) args[1];
        byte[] completedMessage = new byte[]{
            GFX_DRIVER, 0x06, IKE_SYSTEM, OBCRequestSet, 0x01, (byte)hours, (byte)minutes, 0x00
        };
        completedMessage[completedMessage.length - 1] = genMessageCRC(completedMessage);
        return completedMessage;
    }
    
    /**
     * Send a new date setting to the IKE
     * IBus message: 3B 07 80 40 02 <Day> <Month> <Year> <CRC>
     * @param args Three ints MUST be provided
     *     int day, int month, int year
     * @return Byte array of composed message to send to IBus
     */
    public byte[] setDate(Object... args){
        int day = (Integer) args[0];
        int month = (Integer) args[1];
        int year = (Integer) args[2];
        
        byte[] completedMessage = new byte[]{
            GFX_DRIVER, 0x07, IKE_SYSTEM, OBCRequestSet, 0x02, (byte)day, (byte)month, (byte)year, 0x00
        };
        completedMessage[completedMessage.length - 1] = genMessageCRC(completedMessage);
        return completedMessage;
    }
    
    /**
     * Send a new unit setting to the IKE
     * All units must be set at once, oh well.
     * 3B 07 80 15 F2 72 1A 00 33
     * IBus message: 3B 07 80 15 <Vehicle Type/Language> <Units> <Consumption Units> <Engine Type> <CRC>
     * @param args Three ints MUST be provided
     *     int day, int month, int year
     * @return Byte array of composed message to send to IBus
     */
    public byte[] setUnits(Object... args){
        int speedUnit = (Integer) args[0]; // 0 = Km/h 1 = /MPH
        int distanceUnit = (Integer) args[1]; // 0 = Km 1 = Mi
        int tempUnit = (Integer) args[2]; // 0 = C 1 = F
        int dateTimeUnit = (Integer) args[3]; // 0 = 24h 1 = 12h
        int consumptionUnit = (Integer) args[4]; // 0 = L/100 1 = MPG 11 = KM/L
        
        byte allUnits = (byte) Integer.parseInt(
            String.format(
                "%s%s%s%s00%s%s", 
                dateTimeUnit, distanceUnit, distanceUnit, 
                speedUnit, tempUnit, dateTimeUnit 
            ),
            2
        );
        
        String consumptionType = String.format(Locale.US, "%02d", consumptionUnit);
        byte consumptionUnits =(byte) Integer.parseInt(
            String.format(
                "0%s%s%s%s%s", dateTimeUnit, dateTimeUnit, 
                distanceUnit, consumptionType, consumptionType
            ),
            2
        );

        byte[] cmdMsg = new byte[]{
            GFX_DRIVER, 0x07, IKE_SYSTEM, OBCUnitSet, 
            (byte)0xF2, allUnits, consumptionUnits, 0x00, 0x00
        };
        
        cmdMsg[cmdMsg.length - 1] = genMessageCRC(cmdMsg);
        return cmdMsg;
    }
    
    public GFXNavigationSystem(){
        IBusDestinationSystems.put(
            Devices.Radio.toByte(), new RadioSystem()
        );
    }

}
