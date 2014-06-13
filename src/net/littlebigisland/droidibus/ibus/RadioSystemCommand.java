package net.littlebigisland.droidibus.ibus;

import java.util.ArrayList;

/**
 * Handle messages emitted by the Radio unit
 */
class RadioSystemCommand extends IBusSystemCommand{
	
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
						case 0x44: // Stereo Indicator
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
	 * Handle messages broadcast by the Radio
	 */
	class BroadcastSystem extends IBusSystemCommand{
		
		public void mapReceived(ArrayList<Byte> msg){
			currentMessage = msg;
			if(currentMessage.get(3) == 0x02)
				triggerCallback("onUpdateRadioStatus", (int)currentMessage.get(4));
		}
		
	}
	
	/**
	 * Cstruct - Register destination systems
	 */
	RadioSystemCommand(){
		IBusDestinationSystems.put(DeviceAddress.GraphicsNavigationDriver.toByte(), new GFXNavigationSystem());
		IBusDestinationSystems.put(DeviceAddress.Broadcast.toByte(), new BroadcastSystem());
	}
}
