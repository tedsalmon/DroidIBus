package net.littlebigisland.droidibus.ibus.systems;

import java.util.ArrayList;

import net.littlebigisland.droidibus.ibus.DeviceAddressEnum;
import net.littlebigisland.droidibus.ibus.IBusSystemCommand;

public class IKESystemCommand extends IBusSystemCommand {
	
	/**
	 * Handle globally broadcast messages from IKE
	 */
	class IKEGlobalBroadcast extends IBusSystemCommand{
		
		public void mapReceived(ArrayList<Byte> msg){
			switch(msg.get(3)){
				case 0x11: // Ignition State
					int state = (msg.get(4) < 2) ? msg.get(4) : (0x02 & msg.get(4));
					triggerCallback("onUpdateIgnitionSate", state);
					break;
				case 0x15: // Unit Set
					triggerCallback(
						"onUpdateUnits", 
						String.format(
							"%8s;%8s", 
							Integer.toBinaryString(msg.get(5) & 0xFF),
							Integer.toBinaryString(msg.get(6) & 0xFF)
						).replace(' ', '0')
					);
					break;
				case 0x18: // Speed and RPM
					triggerCallback("onUpdateSpeed", (int)msg.get(4));
					triggerCallback("onUpdateRPM", (int)msg.get(5) * 100);
					break;
				case 0x19: // Coolant Temperature
					triggerCallback("onUpdateCoolantTemp", (int)msg.get(5));
					break;
			}
		}
	}
	
	class IKEBroadcast extends IBusSystemCommand{
		private final byte mOBCData = 0x24;
		
		/**
		 * Handle OBC messages sent from IKE
		 * IBus Message: 80 0C FF 24 <System> 00 32 31 3A 31 30 20 20 6E 
		 */
		private void OBCData(){
			// Minus two because the array starts at zero and we need to ignore the last byte (XOR Checksum)
			int endByte = currentMessage.size() - 2;
			switch(currentMessage.get(4)){
				case 0x01: // Time
					String curTime = "";
					// The time contains spaces instead of zeros for hours less than 10
					if(currentMessage.get(endByte) == 0x20){
						curTime = decodeMessage(currentMessage, 6, endByte - 2).replace(" ", "0");
					}else{
						// AM/PM Support
						curTime = decodeMessage(currentMessage, 6, endByte);
					}
					triggerCallback("onUpdateTime", curTime);
					break;
				case 0x02: // Date
					// Dots are retarded and even though this is personal preference I'm coding all
					// The way down here where it doesn't belong (outside of the view) SUE ME.
					String curDate = decodeMessage(currentMessage, 6, endByte).replace(".", "/");
					triggerCallback("onUpdateDate", curDate);
					break;
				case 0x03: //Outdoor Temperature
					// Handle triple digit temperature
					int startByte = (currentMessage.get(6) != 0x20) ? 6 : 7;
					// Clean up the ending space
					endByte = (currentMessage.get(endByte) == 0x20) ? endByte - 1 : endByte; 
					triggerCallback("onUpdateOutdoorTemp", decodeMessage(currentMessage, startByte, endByte));
					break;
				case 0x04: // Fuel 1
					triggerCallback("onUpdateFuel1", decodeMessage(currentMessage, 6, endByte));
					break;
				case 0x05: // Fuel 2
					triggerCallback("onUpdateFuel2", decodeMessage(currentMessage, 6, endByte));
					break;
				case 0x06: // Range
					triggerCallback("onUpdateRange", decodeMessage(currentMessage, 6, endByte));
					break;
				case 0x0A: // AVG Speed
					triggerCallback("onUpdateAvgSpeed", decodeMessage(currentMessage, 6, endByte));
					break;
				case 0x07: // Distance
				case 0x08: // Unknown
				case 0x09: // Limit
				case 0x0E: // Timer
				case 0x0F: // AUX Heater 1
				case 0x10: // AUX Heater 2
					// Not implementing
					break;
			}
		}
		
		public void mapReceived(ArrayList<Byte> msg){
			currentMessage = msg;
			byte operation = msg.get(3);
			if(operation == mOBCData)
				OBCData();
		}
	}
	
	/**
	 * Cstruct - Bind all child classes to the object 
	 */
	public IKESystemCommand(){
		IBusDestinationSystems.put(DeviceAddressEnum.Broadcast.toByte(), new IKEBroadcast());
		IBusDestinationSystems.put(DeviceAddressEnum.GlobalBroadcastAddress.toByte(), new IKEGlobalBroadcast());
	}
}
