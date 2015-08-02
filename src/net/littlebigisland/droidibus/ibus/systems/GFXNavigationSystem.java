package net.littlebigisland.droidibus.ibus.systems;
import java.util.ArrayList;

import net.littlebigisland.droidibus.ibus.IBusSystem;

public class GFXNavigationSystem extends IBusSystem{
    
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
                    byte screen_mode = currentMessage.get(4);
                    triggerCallback("onUpdateScreenState", (int) screen_mode);
                    break;
                case TONE_DATA:
                    int bass = getToneLevel(ToneType.BASS, currentMessage.get(4));
                    int treb = getToneLevel(ToneType.TREB, currentMessage.get(5));
                    int fade = getToneLevel(ToneType.FADE, currentMessage.get(6));
                    int bal = getToneLevel(ToneType.BAL, currentMessage.get(7));
                    triggerCallback("onUpdateToneLevels", bass, treb, fade, bal);
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
    
    public GFXNavigationSystem(){
        IBusDestinationSystems.put(
            Devices.Radio.toByte(), new RadioSystem()
        );
    }

}