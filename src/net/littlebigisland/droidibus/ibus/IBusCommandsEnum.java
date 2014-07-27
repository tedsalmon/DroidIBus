package net.littlebigisland.droidibus.ibus;

/**
 * This should be better explained. Basically, we hold an enum with two values,
 * the source system and the method name that actually creates the data for the method
 * we want. Of course the Source system tells you which class you'll find these methods in.
 * e.x. OnBoardMonitor == BoardMonitorSystemCommand
 */
public enum IBusCommandsEnum {
	// BoardMonitor to IKE Commands
	BMToIKEGetIgnitionStatus(DeviceAddressEnum.OnBoardMonitor, "getIgnitionStatus"),
	BMToIKEGetTime(DeviceAddressEnum.OnBoardMonitor, "getTime"),
	BMToIKEGetDate(DeviceAddressEnum.OnBoardMonitor, "getDate"),
	BMToIKEGetOutdoorTemp(DeviceAddressEnum.OnBoardMonitor, "getOutdoorTemp"),
	BMToIKEGetFuel1(DeviceAddressEnum.OnBoardMonitor, "getFuel1"),
	BMToIKEGetFuel2(DeviceAddressEnum.OnBoardMonitor, "getFuel2"),
	BMToIKEGetRange(DeviceAddressEnum.OnBoardMonitor, "getRange"),
	BMToIKEGetAvgSpeed(DeviceAddressEnum.OnBoardMonitor, "getAvgSpeed"),
	BMToIKEResetFuel1(DeviceAddressEnum.OnBoardMonitor, "resetFuel1"),
	BMToIKEResetFuel2(DeviceAddressEnum.OnBoardMonitor, "resetFuel2"),
	BMToIKEResetAvgSpeed(DeviceAddressEnum.OnBoardMonitor, "resetAvgSpeed"),
	BMToIKESetTime(DeviceAddressEnum.OnBoardMonitor, "setTime"),
	BMToIKESetDate(DeviceAddressEnum.OnBoardMonitor, "setDate"),
	// BoardMonitor to Radio Commands
	BMToRadioGetStatus(DeviceAddressEnum.OnBoardMonitor, "getRadioStatus"),
	BMToRadioCDStatus(DeviceAddressEnum.OnBoardMonitor, "sendCDPlayerMessage"),
	BMToRadioPwrPress(DeviceAddressEnum.OnBoardMonitor, "sendRadioPwrPress"),
	BMToRadioPwrRelease(DeviceAddressEnum.OnBoardMonitor, "sendRadioPwrRelease"),
	BMToRadioTonePress(DeviceAddressEnum.OnBoardMonitor, "sendTonePress"),
	BMToRadioToneRelease(DeviceAddressEnum.OnBoardMonitor, "sendToneRelease"),
	BMToRadioModePress(DeviceAddressEnum.OnBoardMonitor, "sendModePress"),
	BMToRadioModeRelease(DeviceAddressEnum.OnBoardMonitor, "sendModeRelease"),
	BMToRadioVolumeUp(DeviceAddressEnum.OnBoardMonitor, "sendVolumeUp"),
	BMToRadioVolumeDown(DeviceAddressEnum.OnBoardMonitor, "sendVolumeDown"),
	BMToRadioTuneFwdPress(DeviceAddressEnum.OnBoardMonitor, "sendSeekFwdPress"),
	BMToRadioTuneFwdRelease(DeviceAddressEnum.OnBoardMonitor, "sendSeekFwdRelease"),
	BMToRadioTuneRevPress(DeviceAddressEnum.OnBoardMonitor, "sendSeekRevPress"),
	BMToRadioTuneRevRelease(DeviceAddressEnum.OnBoardMonitor, "sendSeekRevRelease"),
	BMToRadioFMPress(DeviceAddressEnum.OnBoardMonitor, "sendFMPress"),
	BMToRadioFMRelease(DeviceAddressEnum.OnBoardMonitor, "sendFMRelease"),
	BMToRadioAMPress(DeviceAddressEnum.OnBoardMonitor, "sendAMPress"),
	BMToRadioAMRelease(DeviceAddressEnum.OnBoardMonitor, "sendAMRelease"),
	BMToRadioInfoPress(DeviceAddressEnum.OnBoardMonitor, "sendInfoPress"),
	BMToRadioInfoRelease(DeviceAddressEnum.OnBoardMonitor, "sendInfoRelease"),
	// BoardMonitor to Light Control Module Commands
	BMToLCMGetDimmerStatus(DeviceAddressEnum.OnBoardMonitor, "getLightDimmerStatus"),
	// BoardMonitor to General Module Commands
	BMToGMGetDoorStatus(DeviceAddressEnum.OnBoardMonitor, "getDoorsRequest"),
	// BoardMonitor to Global Broadcast Address Commands
	BMToGlobalBroadcastAliveMessage(DeviceAddressEnum.OnBoardMonitor, "sendAliveMessage"),
	// Steering Wheel to Radio Commands
	SWToRadioVolumeUp(DeviceAddressEnum.MultiFunctionSteeringWheel, "sendVolumeUp"),
	SWToRadioVolumeDown(DeviceAddressEnum.MultiFunctionSteeringWheel, "sendVolumeDown"),
	SWToRadioTuneFwdPress(DeviceAddressEnum.MultiFunctionSteeringWheel, "sendTuneFwdPress"),
	SWToRadioTuneFwdRelease(DeviceAddressEnum.MultiFunctionSteeringWheel, "sendTuneFwdRelease"),
	SWToRadioTunePrevPress(DeviceAddressEnum.MultiFunctionSteeringWheel, "sendTunePrevPress"),
	SWToRadioTunePrevRelease(DeviceAddressEnum.MultiFunctionSteeringWheel, "sendTunePrevRelease");
	
	private final DeviceAddressEnum system;
	private final String methodName;
	
	IBusCommandsEnum(DeviceAddressEnum system, String methodName) {
	    this.system = system;
	    this.methodName = methodName;
	}

	public DeviceAddressEnum getSystem(){
		return system;
	}
	
	public String getMethodName(){
		return methodName;
	}
}