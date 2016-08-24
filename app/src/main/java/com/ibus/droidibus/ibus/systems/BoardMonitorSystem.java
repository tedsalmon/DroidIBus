package com.ibus.droidibus.ibus.systems;

import java.util.ArrayList;

import com.ibus.droidibus.ibus.IBusSystem;

/**
 * Implements functions of the BoardMonitor. For the most part 
 * we won't be doing any message parsing here since the BoardMonitor
 * is more of an input interface than anything. 
 */
public class BoardMonitorSystem extends IBusSystem{

    // Main Systems
    private final static byte BOARDMONITOR = Devices.BoardMonitor.toByte();
    private final static byte IKE = Devices.InstrumentClusterElectronics.toByte();
    private final static byte RADIO = Devices.Radio.toByte();
    private final static byte GLOBAL = Devices.GlobalBroadcast.toByte();
    private final static byte LCM = Devices.LightControlModule.toByte();
    private final static byte GM_SYSTEM = Devices.BodyModule.toByte();

    
    /**
     * Handle messages destined for the BM from the Radio
     */
    class RadioSystem extends IBusSystem{
        
        public void mapReceived(ArrayList<Byte> msg){
            currentMessage = msg;
            switch(currentMessage.get(3)){
                case 0x4A:
                    // Radio On/Off Status Message
                    // 68 04 F0 4A <data> <CRC>
                    // Suspected states:
                    // 0x00 == Off | 0xFF == Turning on
                    // 0x90 == Radio on? | 0x5F == Loading?
                    // Simple 1 and zero for on and off
                    byte radStat = (byte) currentMessage.get(4);
                    if(radStat == (byte)0xFF || radStat == (byte)0x00){
                        int radioStatus = (radStat == (byte)0xFF) ? 1 : 0;
                        triggerCallback("onUpdateRadioStatus", radioStatus);
                    }
                    // Else it is a state we don't care about
                    break;
                case 0x38:
                    // IBus Message: 68 06 F0 38 00 00 00 A6
                    triggerCallback("onRadioCDStatusRequest");
                    break;
            }
        }
        
    }
    
    /**
     * Issue Get Request to Radio for Status
     * This should be sent every ten seconds
     * @return Byte array of message to send to IBus
     */
    public byte[] getRadioStatus(){
        return new byte[]{
            BOARDMONITOR, 0x03, RADIO, 0x01, (byte)0x9A    
        };
    }
    
    /**
     * Get Door/Flaps Request
     * IBus Message:  F0 03 00 79 8A 
     * @return Byte array of message to send to IBus
     */
    public byte[] getDoorsRequest(){
        return new byte[]{
            BOARDMONITOR, 0x03, GM_SYSTEM, 0x79, (byte) 0x8A
        };
    }
    
    /**
     * Request the current ignition state
     * IBus Message: F0 03 80 10 63
     * @return Byte array of message to send to IBus
     */
    public byte[] getIgnitionStatus(){
        return new byte[]{
            BOARDMONITOR, 0x03, IKE, 0x10, 0x63
        };
    }
    
    /**
     * Request the light dimmer status.
     * Don't ask me how to interpret the results though...
     * IBus Message: F0 03 D0 5D 7E 
     * @return Byte array of message to send to IBus
     */
    public byte[] getLightDimmerStatus(){
        return new byte[]{
            BOARDMONITOR, 0x03, LCM, 0x5D, 0x7E
        };
    }
    
    /**
     * Broadcast BM Alive message. This is the first message sent on boot
     * I believe we register with the Global Master?
     * IBus Message: F0 04 BF 02 70 39
     * @return Byte array of message to send to IBus
     */
    public byte[] sendAliveMessage(){
        return new byte[] {
            BOARDMONITOR, 0x04, GLOBAL, 0x02, 0x70, 0x39
        };
    }
    
    public byte[] sendCDPlayerMessage(Object... args){
        //F0 0B 68 39 00 02 00 01 00 01 08 00 A0
        byte cdNum = (byte)args[1];
        byte trackNum = (byte)args[2];
        int status = (int)args[0];
        byte function = 0x00;
        byte playAvailable = 0x02; // 0x02 is "Not playing"
        switch(status){
            case 1: // Playing song
                playAvailable = 0x09;
                break;
            case 2: // Begin play
                function = 0x02;
                playAvailable = 0x09;
                break;
        }
        byte[] cdStatus = new byte[]{
            BOARDMONITOR, 0x0B, RADIO, 0x39, function, playAvailable, 0x00, 0x01, 0x00, cdNum, trackNum, 0x00, 0x00
        };
        cdStatus[12] = genMessageCRC(cdStatus);
        return cdStatus;
    }
    

    // Radio Buttons
    
    public byte[] sendRadioPwrPress(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, 0x06, (byte)0xD2    
        };
    }
    
    public byte[] sendRadioPwrRelease(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, (byte) 0x86, (byte)0x52    
        };
    }
    
    public byte[] sendModePress(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, 0x23, (byte)0xF7
        };
    }
    
    public byte[] sendModeRelease(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, (byte)0xA3, (byte)0x77
        };
    }
    
    public byte[] sendTonePress(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, 0x04, (byte)0xD0
        };
    }
    
    public byte[] sendToneRelease(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, (byte)0x84, (byte)0x50
        };
    }
    
    public byte[] sendVolumeUp(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x32, (byte)0x21, (byte)0x8F
        };
    }
    
    public byte[] sendVolumeDown(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x32, (byte)0x20, (byte)0x8E
        };
    }
    
    public byte[] sendSeekFwdPress(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, (byte)0x00, (byte)0xD4
        };
    }
    
    public byte[] sendSeekFwdRelease(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, (byte)0x80, (byte)0x54
        };
    }
    
    public byte[] sendSeekRevPress(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, (byte)0x10, (byte)0xC4
        };
    }
    
    public byte[] sendSeekRevRelease(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, (byte)0x90, (byte)0x44
        };
    }
    
    public byte[] sendFMPress(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, (byte)0x31, (byte)0xE5
        };
    }
    
    public byte[] sendFMRelease(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, (byte)0xB1, (byte)0x65
        };
    }
    
    public byte[] sendAMPress(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, (byte)0x21, (byte)0xF5
        };
    }
    
    public byte[] sendAMRelease(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, (byte)0xA1, (byte)0x75
        };
    }
    
    public byte[] sendInfoPress(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, 0x30, (byte)0xE4
        };
    }
    
    public byte[] sendInfoRelease(){
        return new byte[]{
            BOARDMONITOR, 0x04, RADIO, 0x48, (byte)0xB0, 0x64
        };
    }
    
    // F0 04 3B 48 05 82
    public byte[] sendRightPush(){
        return new byte[]{
            BOARDMONITOR, 0x04, 0x3B, 0x48, 0x05, (byte) 0x82
        };
    }
    
    public BoardMonitorSystem(){
        IBusDestinationSystems.put(RADIO, new RadioSystem());
    }
    
}
