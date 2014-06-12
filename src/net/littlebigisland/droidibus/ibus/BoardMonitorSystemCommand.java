package net.littlebigisland.droidibus.ibus;

/**
 * Implements functions of the BoardMonitor. For the most part 
 * we won't be doing any message parsing here since the BoardMonitor
 * is more of an input interface than anything. 
 */
public class BoardMonitorSystemCommand extends IBusSystemCommand {

	// Main Systems
	private byte boardMonitor = DeviceAddress.OnBoardMonitor.toByte();
	private byte gfxDriver = DeviceAddress.GraphicsNavigationDriver.toByte();
	private byte IKESystem = DeviceAddress.InstrumentClusterElectronics.toByte();
	private byte radioSystem = DeviceAddress.Radio.toByte();
	
	// OBC Functions
	private byte OBCRequest = 0x41;
	private byte OBCRequestGet = 0x01;
	private byte OBCRequestReset = 0x10;
	
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

	// Radio Buttons
	public byte[] getRadioStatus(){
		return new byte[]{
			boardMonitor, 0x04, radioSystem, 0x30, (byte)0xE4
		};
	}
	
	public byte[] sendModePress(){
		return new byte[]{
			boardMonitor, 0x04, radioSystem, 0x48, 0x23, (byte)0xF7
		};
	}
	
	public byte[] sendModeRelease(){
		return new byte[]{
			boardMonitor, 0x04, radioSystem, 0x48, (byte)0xA3, (byte)0xFF
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
}
