package net.littlebigisland.droidibus.ibus;

/**
 * This should be better explained. Basically, we hold an enum with two values,
 * the source system and the method name that actually creates the data for the method
 * we want. Of course the Source system tells you which class you'll find these methods in.
 * e.x. BoardMonitor == BoardMonitorSystemCommand
 */
public enum IBusCommandsEnum {
	// BoardMonitor to IKE Commands
	BMToIKEGetIgnitionStatus(DeviceAddressEnum.BoardMonitor, "getIgnitionStatus"),
	BMToIKEGetTime(DeviceAddressEnum.BoardMonitor, "getTime"),
	BMToIKEGetDate(DeviceAddressEnum.BoardMonitor, "getDate"),
	BMToIKEGetOutdoorTemp(DeviceAddressEnum.BoardMonitor, "getOutdoorTemp"),
	BMToIKEGetFuel1(DeviceAddressEnum.BoardMonitor, "getFuel1"),
	BMToIKEGetFuel2(DeviceAddressEnum.BoardMonitor, "getFuel2"),
	BMToIKEGetRange(DeviceAddressEnum.BoardMonitor, "getRange"),
	BMToIKEGetAvgSpeed(DeviceAddressEnum.BoardMonitor, "getAvgSpeed"),
	BMToIKEResetFuel1(DeviceAddressEnum.BoardMonitor, "resetFuel1"),
	BMToIKEResetFuel2(DeviceAddressEnum.BoardMonitor, "resetFuel2"),
	BMToIKEResetAvgSpeed(DeviceAddressEnum.BoardMonitor, "resetAvgSpeed"),
	BMToIKESetTime(DeviceAddressEnum.BoardMonitor, "setTime"),
	BMToIKESetDate(DeviceAddressEnum.BoardMonitor, "setDate"),
	BMToIKESetUnits(DeviceAddressEnum.BoardMonitor, "setUnits"),
	// BoardMonitor to Radio Commands
	BMToRadioGetStatus(DeviceAddressEnum.BoardMonitor, "getRadioStatus"),
	BMToRadioCDStatus(DeviceAddressEnum.BoardMonitor, "sendCDPlayerMessage"),
	BMToRadioPwrPress(DeviceAddressEnum.BoardMonitor, "sendRadioPwrPress"),
	BMToRadioPwrRelease(DeviceAddressEnum.BoardMonitor, "sendRadioPwrRelease"),
	BMToRadioTonePress(DeviceAddressEnum.BoardMonitor, "sendTonePress"),
	BMToRadioToneRelease(DeviceAddressEnum.BoardMonitor, "sendToneRelease"),
	BMToRadioModePress(DeviceAddressEnum.BoardMonitor, "sendModePress"),
	BMToRadioModeRelease(DeviceAddressEnum.BoardMonitor, "sendModeRelease"),
	BMToRadioVolumeUp(DeviceAddressEnum.BoardMonitor, "sendVolumeUp"),
	BMToRadioVolumeDown(DeviceAddressEnum.BoardMonitor, "sendVolumeDown"),
	BMToRadioTuneFwdPress(DeviceAddressEnum.BoardMonitor, "sendSeekFwdPress"),
	BMToRadioTuneFwdRelease(DeviceAddressEnum.BoardMonitor, "sendSeekFwdRelease"),
	BMToRadioTuneRevPress(DeviceAddressEnum.BoardMonitor, "sendSeekRevPress"),
	BMToRadioTuneRevRelease(DeviceAddressEnum.BoardMonitor, "sendSeekRevRelease"),
	BMToRadioFMPress(DeviceAddressEnum.BoardMonitor, "sendFMPress"),
	BMToRadioFMRelease(DeviceAddressEnum.BoardMonitor, "sendFMRelease"),
	BMToRadioAMPress(DeviceAddressEnum.BoardMonitor, "sendAMPress"),
	BMToRadioAMRelease(DeviceAddressEnum.BoardMonitor, "sendAMRelease"),
	BMToRadioInfoPress(DeviceAddressEnum.BoardMonitor, "sendInfoPress"),
	BMToRadioInfoRelease(DeviceAddressEnum.BoardMonitor, "sendInfoRelease"),
	// BoardMonitor to Light Control Module Commands
	BMToLCMGetDimmerStatus(DeviceAddressEnum.BoardMonitor, "getLightDimmerStatus"),
	// BoardMonitor to General Module Commands
	BMToGMGetDoorStatus(DeviceAddressEnum.BoardMonitor, "getDoorsRequest"),
	// BoardMonitor to Global Broadcast Address Commands
	BMToGlobalBroadcastAliveMessage(DeviceAddressEnum.BoardMonitor, "sendAliveMessage"),
	// Steering Wheel to Radio Commands
	SWToRadioVolumeUp(DeviceAddressEnum.MultiFunctionSteeringWheel, "sendVolumeUp"),
	SWToRadioVolumeDown(DeviceAddressEnum.MultiFunctionSteeringWheel, "sendVolumeDown"),
	SWToRadioTuneFwdPress(DeviceAddressEnum.MultiFunctionSteeringWheel, "sendTuneFwdPress"),
	SWToRadioTuneFwdRelease(DeviceAddressEnum.MultiFunctionSteeringWheel, "sendTuneFwdRelease"),
	SWToRadioTuneRevPress(DeviceAddressEnum.MultiFunctionSteeringWheel, "sendTunePrevPress"),
	SWToRadioTuneRevRelease(DeviceAddressEnum.MultiFunctionSteeringWheel, "sendTunePrevRelease");
	
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