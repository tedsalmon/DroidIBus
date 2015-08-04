package net.littlebigisland.droidibus.ibus;

/**
 * This class will be used to send commands to the IBusService. 
 * This allows us to hold optional arguments for methods we call.
 */
public class IBusCommand{
    
    public Commands commandType = null;
    public Object commandArgs = null;
    
    /**
    * This should be better explained. Basically, we hold an enum with two values,
    * the source system and the method name that actually creates the data for the method
    * we want. Of course the Source system tells you which class you'll find these methods in.
    * e.x. BoardMonitor == BoardMonitorSystemCommand
    */
    public enum Commands{
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
        // GFX Navigation Driver to IKE Commands
        GFXToIKEGetIgnitionStatus(IBusSystem.Devices.GFXNavigationDriver, "getIgnitionStatus"),
        GFXToIKEGetTime(IBusSystem.Devices.GFXNavigationDriver, "getTime"),
        GFXToIKEGetDate(IBusSystem.Devices.GFXNavigationDriver, "getDate"),
        GFXToIKEGetOutdoorTemp(IBusSystem.Devices.GFXNavigationDriver, "getOutdoorTemp"),
        GFXToIKEGetFuel1(IBusSystem.Devices.GFXNavigationDriver, "getFuel1"),
        GFXToIKEGetFuel2(IBusSystem.Devices.GFXNavigationDriver, "getFuel2"),
        GFXToIKEGetRange(IBusSystem.Devices.GFXNavigationDriver, "getRange"),
        GFXToIKEGetAvgSpeed(IBusSystem.Devices.GFXNavigationDriver, "getAvgSpeed"),
        GFXToIKEResetFuel1(IBusSystem.Devices.GFXNavigationDriver, "resetFuel1"),
        GFXToIKEResetFuel2(IBusSystem.Devices.GFXNavigationDriver, "resetFuel2"),
        GFXToIKEResetAvgSpeed(IBusSystem.Devices.BoardMonitor, "resetAvgSpeed"),
        GFXToIKESetTime(IBusSystem.Devices.GFXNavigationDriver, "setTime"),
        GFXToIKESetDate(IBusSystem.Devices.GFXNavigationDriver, "setDate"),
        GFXToIKESetUnits(IBusSystem.Devices.GFXNavigationDriver, "setUnits"),
        // GlobalBroadcast to IKE commands
        GBCToIKEGetMilage(IBusSystem.Devices.GlobalBroadcast, "getMileage"),
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