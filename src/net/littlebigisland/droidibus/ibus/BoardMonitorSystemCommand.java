package net.littlebigisland.droidibus.ibus;

import java.math.BigInteger;

/**
 * Implements functions of the BoardMonitor. For the most part 
 * we won't be doing any message parsing here since the BoardMonitor
 * is more of an input interface than anything. 
 */
public class BoardMonitorSystemCommand extends IBusSystemCommand {

	// Main Systems
	private byte boardMonitor = DeviceAddressEnum.OnBoardMonitor.toByte();
	private byte gfxDriver = DeviceAddressEnum.GraphicsNavigationDriver.toByte();
	private byte IKESystem = DeviceAddressEnum.InstrumentClusterElectronics.toByte();
	private byte radioSystem = DeviceAddressEnum.Radio.toByte();
	
	// OBC Functions
	private byte OBCRequest = 0x41;
	private byte OBCRequestGet = 0x01;
	private byte OBCRequestReset = 0x10;
	private byte OBCRequestSet = 0x40;
	private byte OBCUnitSet = 0x15;
	
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
			gfxDriver, 0x06, IKESystem, OBCRequestSet, (byte)hours, (byte)minutes, 0x00
		};
		completedMessage[6] = genMessageCRC(completedMessage);
		return completedMessage;
	}
	
	/**
	 * Send a new date setting to the IKE
	 * IBus message: 3B 06 80 40 02 <Day> <Month> <Year> <CRC>
	 * @param args Three ints MUST be provided
	 * 	int day, int month, int year
	 * @return Byte array of composed message to send to IBus
	 */
	public byte[] setDate(Object... args){
		int day = (Integer) args[0];
		int month = (Integer) args[1];
		int year = (Integer) args[2];
		
		byte[] completedMessage = new byte[]{
			gfxDriver, 0x07, IKESystem, OBCRequestSet, (byte)day, (byte)month, (byte)year, 0x00
		};
		completedMessage[7] = genMessageCRC(completedMessage);
		return completedMessage;
	}
	
	/**
	 * Send a new unit setting to the IKE
	 * All units must be set at once, oh well.
	 * IBus message: 3B 07 80 15 <Vehicle Type/Language> <Units> <Consumption Units> <Engine Type> <CRC>
	 * @param args Three ints MUST be provided
	 * 	int day, int month, int year
	 * @return Byte array of composed message to send to IBus
	 */
	public byte[] setUnits(Object... args){
		int speedUnit = (Integer) args[0]; // 0 = KM/Km/h 1= Miles/MPH
		int tempUnit = (Integer) args[1]; // 0 = C 1 = F
		int dateTimeUnit = (Integer) args[2]; // 0 = 24h 1 = 12h
		
		// TODO Finish implementation
		byte[] allUnits = new BigInteger(
			String.format("%s%s00%s%s%s%s",dateTimeUnit, tempUnit, speedUnit, speedUnit, speedUnit, dateTimeUnit ),
		2).toByteArray();
		String consumptionType = (speedUnit == 0) ? "11" : "01";
		byte[] consumptionUnits = new BigInteger(
			String.format("0000%s%s", consumptionType, consumptionType),
		2).toByteArray();

		byte[] completedMessage = new byte[]{
			gfxDriver, 0x07, IKESystem, OBCUnitSet, 0x00, allUnits[1], consumptionUnits[1], 0x00, 0x00
		};
		
		completedMessage[8] = genMessageCRC(completedMessage);
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
	
}
