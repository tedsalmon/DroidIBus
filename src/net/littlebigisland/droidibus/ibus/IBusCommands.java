package net.littlebigisland.droidibus.ibus;

public enum IBusCommands {
	
	IKEGetFuel(DeviceAddress.InstrumentClusterElectronics.toByte(), 0);
	
	private final byte targetSystem;
	private final int functionID;
	
	IBusCommands(byte ts, int fid){
		targetSystem = ts;
		functionID = fid;
	}
	
	public byte getTargetSystem(){
		return targetSystem;
	}
	
	public int getFunctionID(){
		return functionID;
	}
}
