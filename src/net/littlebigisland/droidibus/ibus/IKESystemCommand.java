package net.littlebigisland.droidibus.ibus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;

public class IKESystemCommand extends IBusSystemCommand {
	
	private Map<Byte, IBusSystemCommand> IBusIKEMap = new HashMap<Byte, IBusSystemCommand>();
	
	/**
	 * Handle globally broadcast messages from IKE
	 */
	class IKEGlobalBroadcast extends IBusSystemCommand{
		
		public void mapReceived(ArrayList<Byte> msg){
			switch(msg.get(3)){
				case 0x11: // Ignition State
					int state = (msg.get(3) < 3) ? msg.get(3) : (0x02 & msg.get(3));
					if(mCallbackReceiver != null)
						mCallbackReceiver.onUpdateIgnitionSate(state);
					break;
				case 0x18: // Speed and RPM
					int speedInMPH = ((int) ((msg.get(4) * 2) * 0.621371));
					if(mCallbackReceiver != null)
						mCallbackReceiver.onUpdateSpeed(
							String.format("%s mph", speedInMPH)
						);
					mCallbackReceiver.onUpdateRPM(
							String.format("%s", (int) msg.get(5) * 100)
						);
					break;
				case 0x19: // Coolant Temperature
					if(mCallbackReceiver != null)
						mCallbackReceiver.onUpdateCoolantTemp(
							String.format("%s C", (int) msg.get(5))
						);
					break;
			}
		}
	}
	
	class IKEBroadcast extends IBusSystemCommand{
		private ArrayList<Byte> currentMessage;
		private final byte OBCData = 0x24;
		
		/**
		 * Handle OBC messages sent from IKE
		 */
		private void OBCData(){
			// Minus two because the array starts at zero and we need to ignore the last byte (XOR Checksum)
			int endByte = currentMessage.size() - 2;
			Log.d("DroidIBus", String.format("IKE OBC Type is 0x%02X", currentMessage.get(4)));
			switch(currentMessage.get(4)){
				case 0x01: //Time
					if(mCallbackReceiver != null)
						mCallbackReceiver.onUpdateTime(
								decodeMessage.decode(currentMessage, 6, endByte)
						);
					break;
				case 0x02: //Date
					if(mCallbackReceiver != null)
						mCallbackReceiver.onUpdateDate(
								decodeMessage.decode(currentMessage, 6, endByte)
						);
					break;
				case 0x03: //Outdoor Temperature
					if(mCallbackReceiver != null)
						mCallbackReceiver.onUpdateOutdoorTemp(
							decodeMessage.decode(currentMessage, 7, endByte)
						);
					break;
				case 0x04: // Fuel 1
					if(mCallbackReceiver != null)
						mCallbackReceiver.onUpdateFuel1(
							decodeMessage.decode(currentMessage, 6, endByte)
						);
					break;
				case 0x05: // Fuel 2
					if(mCallbackReceiver != null)
						mCallbackReceiver.onUpdateFuel2(
							decodeMessage.decode(currentMessage, 6, endByte)
						);
					break;
				case 0x06: // Range
					if(mCallbackReceiver != null)
						mCallbackReceiver.onUpdateRange(
							decodeMessage.decode(currentMessage, 6, endByte)
						);
					break;
				case 0x0A: // AVG Speed
					if(mCallbackReceiver != null)
						mCallbackReceiver.onUpdateAvgSpeed(
							decodeMessage.decode(currentMessage, 6, endByte)
						);
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
			if(operation == OBCData){
				OBCData();
			}
		}
	}
	
	public void mapReceived(ArrayList<Byte> msg){
		if(IBusIKEMap.isEmpty()){
			IBusIKEMap.put(DeviceAddress.Broadcast.toByte(), new IKEBroadcast());
			IBusIKEMap.put(DeviceAddress.GlobalBroadcastAddress.toByte(), new IKEGlobalBroadcast());
		}
		try{
			IBusIKEMap.get((byte) msg.get(2)).mapReceived(msg);
		}catch(NullPointerException npe){
			// Things not in the map throw a NullPointerException
		}
	}
}
