package net.littlebigisland.droidibus.ibus.systems;

import java.util.ArrayList;

import net.littlebigisland.droidibus.ibus.DeviceAddressEnum;
import net.littlebigisland.droidibus.ibus.IBusSystemCommand;

/**
 * Handle messages emitted by the Radio unit
 */
public class RadioSystemCommand extends IBusSystemCommand{
	
	/**
	 * Handle messages bound for the BoardMonitor from the Radio in the trunk
	 */
	class GFXNavigationSystem extends IBusSystemCommand{
		
		private final byte stationText = 0x23;
		private final byte metaData = (byte)0xA5;
		
		public void mapReceived(ArrayList<Byte> msg){
			currentMessage = msg;
			
			switch(currentMessage.get(3)){
				case stationText:
					triggerCallback("onUpdateRadioStation", decodeData());
					break;
				case metaData:
					byte metaDataType = currentMessage.get(6);
					String dataText = decodeData();

					switch(metaDataType){
						case 0x41: // Broadcast Type
							triggerCallback("onUpdateRadioBrodcasts", dataText);
							break;
						case 0x04:
						case 0x44: // Stereo Indicators
							triggerCallback("onUpdateRadioStereoIndicator", dataText);
							break;
						case 0x45: // Radio Data System Indicator
							triggerCallback("onUpdateRadioRDSIndicator", dataText);
							break;
						case 0x02: // Program Type
							triggerCallback("onUpdateRadioProgramIndicator", dataText);
							break;
					}
					break;
			}

		}
		
		/**
		 * Iterate through the message and remove all non-ASCII data
		 * 
		 * @return String 	String representation of data
		 */
		private String decodeData(){
			int startByte = (currentMessage.get(3) == stationText) ? 6 : 7; // Radio RDS starts 6 bytes in, metadata at 7
			int endByte = currentMessage.size() - 2; // Skip the CRC
			
			// Remove the padding from the front and back of the message
			while(currentMessage.get(endByte) == 0x20)
				endByte--;
			while(currentMessage.get(startByte) == 0x20)
				startByte++;
			return decodeMessage(currentMessage, startByte, endByte);			
		}
		
	}
	
	/**
	 * Handle messages destined for the OnBoardMonitor
	 */
	class OnBoardMonitorSystem extends IBusSystemCommand{
		
		public void mapReceived(ArrayList<Byte> msg){
			currentMessage = msg;
			switch(currentMessage.get(3)){
				case 0x4A: // Radio On/Off Status Message
					// 68 04 F0 4A <data> <CRC>
					// I believe 0x00 is the broadcasted state when the radio is coming online?
					// Either way it's not something we want to catch as it'll throw off our states
					if(currentMessage.get(4) != (byte) 0x00){
						// 1 for on, 0 for off
						int radioStatus = (currentMessage.get(4) == (byte)0xFF) ? 1 : 0;
						triggerCallback("onUpdateRadioStatus", radioStatus);
					}
					break;
				case 0x38:
					// IBus Message: 68 06 F0 38 00 00 00 A6
					triggerCallback("onRadioCDStatusRequest");
					break;
			}
		}
		
	}
	
	/**
	 * Cstruct - Register destination systems
	 */
	public RadioSystemCommand(){
		IBusDestinationSystems.put(DeviceAddressEnum.GraphicsNavigationDriver.toByte(), new GFXNavigationSystem());
		IBusDestinationSystems.put(DeviceAddressEnum.OnBoardMonitor.toByte(), new OnBoardMonitorSystem());
	}
}
