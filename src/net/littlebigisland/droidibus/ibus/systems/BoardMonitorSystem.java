package net.littlebigisland.droidibus.ibus.systems;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import net.littlebigisland.droidibus.ibus.IBusSystem;

/**
 * Implements functions of the BoardMonitor. For the most part 
 * we won't be doing any message parsing here since the BoardMonitor
 * is more of an input interface than anything. 
 */
public class BoardMonitorSystem extends IBusSystem{

    // Main Systems
    private byte boardMonitor = Devices.BoardMonitor.toByte();
    private byte gfxDriver = Devices.GFXNavigationDriver.toByte();
    private byte IKESystem = Devices.InstrumentClusterElectronics.toByte();
    private byte radioSystem = Devices.Radio.toByte();
    private byte globalSystem = Devices.GlobalBroadcast.toByte();
    private byte lightControlSystem = Devices.LightControlModule.toByte();
    private byte generalModuleSystem = Devices.BodyModule.toByte();
    
    // OBC Functions
    private byte OBCRequest = 0x41;
    private byte OBCRequestGet = 0x01;
    private byte OBCRequestReset = 0x10;
    private byte OBCRequestSet = 0x40;
    private byte OBCUnitSet = 0x15;
    
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
     * Generate an IKE message requesting a value reset for the given system.
     * @param system The hex value of the system in question
     * @param checksum The hex value of the message checksum
     * @return Byte array of message to send to IBus
     */
    private byte[] IKEGetRequest(int system, int checksum){
        return new byte[] {
            gfxDriver, 0x05, IKESystem, 
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
            gfxDriver, 0x05, IKESystem, 
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
     * Issue Get Request to Radio for Status
     * This should be sent every ten seconds
     * @return Byte array of message to send to IBus
     */
    public byte[] getRadioStatus(){
        return new byte[]{
            boardMonitor, 0x03, radioSystem, 0x01, (byte)0x9A    
        };
    }
    
    /**
     * Get Door/Flaps Request
     * IBus Message:  F0 03 00 79 8A 
     * @return Byte array of message to send to IBus
     */
    public byte[] getDoorsRequest(){
        return new byte[]{
            boardMonitor, 0x03, generalModuleSystem, 0x79, (byte) 0x8A
        };
    }
    
    /**
     * Request the current ignition state
     * IBus Message: F0 03 80 10 63
     * @return Byte array of message to send to IBus
     */
    public byte[] getIgnitionStatus(){
        return new byte[]{
            boardMonitor, 0x03, IKESystem, 0x10, 0x63
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
            boardMonitor, 0x03, lightControlSystem, 0x5D, 0x7E
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
            boardMonitor, 0x04, globalSystem, 0x02, 0x70, 0x39
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
            boardMonitor, 0x0B, radioSystem, 0x39, function, playAvailable, 0x00, 0x01, 0x00, cdNum, trackNum, 0x00, 0x00
        };
        cdStatus[12] = genMessageCRC(cdStatus);
        return cdStatus;
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
            gfxDriver, 0x06, IKESystem, OBCRequestSet, 0x01, (byte)hours, (byte)minutes, 0x00
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
            gfxDriver, 0x07, IKESystem, OBCRequestSet, 0x02, (byte)day, (byte)month, (byte)year, 0x00
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
    @SuppressLint("DefaultLocale") // TODO Fix me
    public byte[] setUnits(Object... args){
        int speedUnit = (Integer) args[0]; // 0 = Km/h 1 = /MPH
        int distanceUnit = (Integer) args[1]; // 0 = Km 1 = Mi
        int tempUnit = (Integer) args[2]; // 0 = C 1 = F
        int dateTimeUnit = (Integer) args[3]; // 0 = 24h 1 = 12h
        int consumptionUnit = (Integer) args[4]; // 0 = L/100 1 = MPG 11 = KM/L
        
        byte allUnits = (byte) Integer.parseInt(
            String.format(
                "%s%s%s%s00%s%s", dateTimeUnit, distanceUnit, distanceUnit, speedUnit, tempUnit, dateTimeUnit 
            ),
            2
        );
        
        String consumptionType = String.format("%02d", consumptionUnit);
        byte consumptionUnits =(byte) Integer.parseInt(
            String.format("0%s%s%s%s%s", dateTimeUnit, dateTimeUnit, distanceUnit, consumptionType, consumptionType),
            2
        );

        byte[] completedMessage = new byte[]{
            gfxDriver, 0x07, IKESystem, OBCUnitSet, (byte)0xF2, allUnits, consumptionUnits, 0x00, 0x00
        };
        
        completedMessage[completedMessage.length - 1] = genMessageCRC(completedMessage);
        return completedMessage;
    }
    

    // Radio Buttons
    
    public byte[] sendRadioPwrPress(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, 0x06, (byte)0xD2    
        };
    }
    
    public byte[] sendRadioPwrRelease(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, (byte) 0x86, (byte)0x52    
        };
    }
    
    public byte[] sendModePress(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, 0x23, (byte)0xF7
        };
    }
    
    public byte[] sendModeRelease(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, (byte)0xA3, (byte)0x77
        };
    }
    
    public byte[] sendTonePress(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, 0x04, (byte)0xD0
        };
    }
    
    public byte[] sendToneRelease(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, (byte)0x84, (byte)0x50
        };
    }
    
    public byte[] sendVolumeUp(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x32, (byte)0x21, (byte)0x8F
        };
    }
    
    public byte[] sendVolumeDown(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x32, (byte)0x20, (byte)0x8E
        };
    }
    
    public byte[] sendSeekFwdPress(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, (byte)0x00, (byte)0xD4
        };
    }
    
    public byte[] sendSeekFwdRelease(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, (byte)0x80, (byte)0x54
        };
    }
    
    public byte[] sendSeekRevPress(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, (byte)0x10, (byte)0xC4
        };
    }
    
    public byte[] sendSeekRevRelease(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, (byte)0x90, (byte)0x44
        };
    }
    
    public byte[] sendFMPress(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, (byte)0x31, (byte)0xE5
        };
    }
    
    public byte[] sendFMRelease(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, (byte)0xB1, (byte)0x65
        };
    }
    
    public byte[] sendAMPress(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, (byte)0x21, (byte)0xF5
        };
    }
    
    public byte[] sendAMRelease(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, (byte)0xA1, (byte)0x75
        };
    }
    
    public byte[] sendInfoPress(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, 0x30, (byte)0xE4
        };
    }
    
    public byte[] sendInfoRelease(){
        return new byte[]{
            boardMonitor, 0x04, radioSystem, 0x48, (byte)0xB0, 0x64
        };
    }
    
    public BoardMonitorSystem(){
        IBusDestinationSystems.put(radioSystem, new RadioSystem());
    }
    
}
