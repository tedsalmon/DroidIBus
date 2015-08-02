package net.littlebigisland.droidibus.ibus;

/**
 * This class will be used to send commands to the IBusService. 
 * This allows us to hold optional arguments for methods we call.
 */
public class IBusCommand{
    
    Commands commandType = null;
    Object commandArgs = null;
    
    /**
    * This should be better explained. Basically, we hold an enum with two values,
    * the source system and the method name that actually creates the data for the method
    * we want. Of course the Source system tells you which class you'll find these methods in.
    * e.x. BoardMonitor == BoardMonitorSystemCommand
    */
    public enum Commands{
        // BoardMonitor to IKE Commands
        BMToIKEGetIgnitionStatus(IBusSystem.Devices.BoardMonitor, "getIgnitionStatus"),
        BMToIKEGetTime(IBusSystem.Devices.BoardMonitor, "getTime"),
        BMToIKEGetDate(IBusSystem.Devices.BoardMonitor, "getDate"),
        BMToIKEGetOutdoorTemp(IBusSystem.Devices.BoardMonitor, "getOutdoorTemp"),
        BMToIKEGetFuel1(IBusSystem.Devices.BoardMonitor, "getFuel1"),
        BMToIKEGetFuel2(IBusSystem.Devices.BoardMonitor, "getFuel2"),
        BMToIKEGetRange(IBusSystem.Devices.BoardMonitor, "getRange"),
        BMToIKEGetAvgSpeed(IBusSystem.Devices.BoardMonitor, "getAvgSpeed"),
        BMToIKEResetFuel1(IBusSystem.Devices.BoardMonitor, "resetFuel1"),
        BMToIKEResetFuel2(IBusSystem.Devices.BoardMonitor, "resetFuel2"),
        BMToIKEResetAvgSpeed(IBusSystem.Devices.BoardMonitor, "resetAvgSpeed"),
        BMToIKESetTime(IBusSystem.Devices.BoardMonitor, "setTime"),
        BMToIKESetDate(IBusSystem.Devices.BoardMonitor, "setDate"),
        BMToIKESetUnits(IBusSystem.Devices.BoardMonitor, "setUnits"),
        // BoardMonitor to Radio Commands
        BMToRadioGetStatus(IBusSystem.Devices.BoardMonitor, "getRadioStatus"),
        BMToRadioCDStatus(IBusSystem.Devices.BoardMonitor, "sendCDPlayerMessage"),
        BMToRadioPwrPress(IBusSystem.Devices.BoardMonitor, "sendRadioPwrPress"),
        BMToRadioPwrRelease(IBusSystem.Devices.BoardMonitor, "sendRadioPwrRelease"),
        BMToRadioTonePress(IBusSystem.Devices.BoardMonitor, "sendTonePress"),
        BMToRadioToneRelease(IBusSystem.Devices.BoardMonitor, "sendToneRelease"),
        BMToRadioModePress(IBusSystem.Devices.BoardMonitor, "sendModePress"),
        BMToRadioModeRelease(IBusSystem.Devices.BoardMonitor, "sendModeRelease"),
        BMToRadioVolumeUp(IBusSystem.Devices.BoardMonitor, "sendVolumeUp"),
        BMToRadioVolumeDown(IBusSystem.Devices.BoardMonitor, "sendVolumeDown"),
        BMToRadioTuneFwdPress(IBusSystem.Devices.BoardMonitor, "sendSeekFwdPress"),
        BMToRadioTuneFwdRelease(IBusSystem.Devices.BoardMonitor, "sendSeekFwdRelease"),
        BMToRadioTuneRevPress(IBusSystem.Devices.BoardMonitor, "sendSeekRevPress"),
        BMToRadioTuneRevRelease(IBusSystem.Devices.BoardMonitor, "sendSeekRevRelease"),
        BMToRadioFMPress(IBusSystem.Devices.BoardMonitor, "sendFMPress"),
        BMToRadioFMRelease(IBusSystem.Devices.BoardMonitor, "sendFMRelease"),
        BMToRadioAMPress(IBusSystem.Devices.BoardMonitor, "sendAMPress"),
        BMToRadioAMRelease(IBusSystem.Devices.BoardMonitor, "sendAMRelease"),
        BMToRadioInfoPress(IBusSystem.Devices.BoardMonitor, "sendInfoPress"),
        BMToRadioInfoRelease(IBusSystem.Devices.BoardMonitor, "sendInfoRelease"),
        // BoardMonitor to Light Control Module Commands
        BMToLCMGetDimmerStatus(IBusSystem.Devices.BoardMonitor, "getLightDimmerStatus"),
        // BoardMonitor to General Module Commands
        BMToGMGetDoorStatus(IBusSystem.Devices.BoardMonitor, "getDoorsRequest"),
        // BoardMonitor to Global Broadcast Address Commands
        BMToGlobalBroadcastAliveMessage(IBusSystem.Devices.BoardMonitor, "sendAliveMessage"),
        
        // Global Broadcast to IKE Commands
        GlobalBroadcastToIKEGetMileage(IBusSystem.Devices.GlobalBroadcast, "getMileage"),
        
        // Steering Wheel to Radio Commands
        SWToRadioVolumeUp(IBusSystem.Devices.MultiFunctionSteeringWheel, "sendVolumeUp"),
        SWToRadioVolumeDown(IBusSystem.Devices.MultiFunctionSteeringWheel, "sendVolumeDown"),
        SWToRadioTuneFwdPress(IBusSystem.Devices.MultiFunctionSteeringWheel, "sendTuneFwdPress"),
        SWToRadioTuneFwdRelease(IBusSystem.Devices.MultiFunctionSteeringWheel, "sendTuneFwdRelease"),
        SWToRadioTuneRevPress(IBusSystem.Devices.MultiFunctionSteeringWheel, "sendTunePrevPress"),
        SWToRadioTuneRevRelease(IBusSystem.Devices.MultiFunctionSteeringWheel, "sendTunePrevRelease");
        
        private final IBusSystem.Devices system;
        private final String methodName;
        
        Commands(IBusSystem.Devices system, String methodName) {
            this.system = system;
            this.methodName = methodName;
        }
        
        public IBusSystem.Devices getSystem(){
            return system;
        }
        
        public String getMethodName(){
            return methodName;
        }
    }
    
    public IBusCommand(Commands cmd, Object... args){
        commandType = cmd;
        if(args.length > 0){
            commandArgs = args;
        }
    }
}