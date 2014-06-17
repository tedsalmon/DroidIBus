package net.littlebigisland.droidibus.ibus;

public enum IBusCommandsEnum {
	//BoardMonitor to IKE Commands
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
	//BoardMonitor to Radio Commands
	BMToRadioGetStatus(DeviceAddressEnum.OnBoardMonitor, "getRadioStatus"),
	BMToRadioPwrPress(DeviceAddressEnum.OnBoardMonitor, ""),
	BMToRadioPwrRelease(DeviceAddressEnum.OnBoardMonitor, ""),
	BMToRadioTonePress(DeviceAddressEnum.OnBoardMonitor, ""),
	BMToRadioToneRelease(DeviceAddressEnum.OnBoardMonitor, ""),
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
	BMToRadioInfoRelease(DeviceAddressEnum.OnBoardMonitor, "sendInfoRelease");	
	
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