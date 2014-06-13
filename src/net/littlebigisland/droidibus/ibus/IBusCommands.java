package net.littlebigisland.droidibus.ibus;

public enum IBusCommands {
	//BoardMonitor to IKE Commands
	BMToIKEGetTime,
	BMToIKEGetDate,
	BMToIKEGetOutdoorTemp,
	BMToIKEGetFuel1,
	BMToIKEGetFuel2,
	BMToIKEGetRange,
	BMToIKEGetAvgSpeed,
	BMToIKEResetFuel1,
	BMToIKEResetFuel2,
	BMToIKEResetAvgSpeed,
	//BoardMonitor to Radio Commands
	BMToRadioModePress,
	BMToRadioModeRelease,
	BMToRadioVolumeUp,
	BMToRadioVolumeDown,
	BMToRadioTuneFwdPress,
	BMToRadioTuneFwdRelease,
	BMToRadioTuneRevPress,
	BMToRadioTuneRevRelease,
	BMToRadioFMPress,
	BMToRadioFMRelease,
	BMToRadioAMPress,
	BMToRadioAMRelease,
	BMToRadioInfoPress,
	BMToRadioInfoRelease,
	BMToRadioGetStatus;
}