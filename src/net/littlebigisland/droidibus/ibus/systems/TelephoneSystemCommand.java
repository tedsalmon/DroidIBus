package net.littlebigisland.droidibus.ibus.systems;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;

import net.littlebigisland.droidibus.ibus.DeviceAddressEnum;
import net.littlebigisland.droidibus.ibus.IBusSystemCommand;

public class TelephoneSystemCommand extends IBusSystemCommand{
	
	/**
	 * 
	 */
	class IKESystem extends IBusSystemCommand{
		
		@SuppressLint("UseSparseArrays")
		private HashMap<Byte, byte[]> byteMapping = new HashMap<Byte, byte[]>();
		
		/**
		 * convertSpecialBytes() - Take the IBus specific bytes that the
		 * LCDs in the car can display and convert them to their UTF 
		 * @param data The data to look over
		 * @param start The first byte to inspect
		 * @param end   The last byte to inspect
		 * @return 		The ArrayList of bytes converted
		 */
		private ArrayList<Byte> convertSpecialBytes(ArrayList<Byte> data, int start, int end){
			ArrayList<Byte> returnData = new ArrayList<Byte>();
			int position = start;
			while(position <= end){
				byte curByte = data.get(position);
				byte[] map = byteMapping.get(curByte);
				if(map != null){
					for(int i = 0; i < map.length; i++){
						returnData.add(map[i]);
					}
				}else{
					returnData.add(curByte);
				}
				position++;
			}
			return returnData;
		}
		
		public void mapReceived(ArrayList<Byte> msg) {
			currentMessage = msg;
			if(currentMessage.get(3) == 0x23){
				// IKE Display C8 12 80 23 42 <DATA> <CRC>
				if(currentMessage.get(4) == 0x42){
					ArrayList<Byte> msgData = convertSpecialBytes(currentMessage, 6,  currentMessage.size()-2);
					triggerCallback("onUpdateIKEDisplay", decodeMessage(msgData, 0, msgData.size()-1));
				}
			}
		}
		
		IKESystem(){
			// Add the bytes we need to convert from the IKE Display to the mapping HashMap
			byteMapping.put((byte) 0xAD, new byte[] {(byte) 0xE2, (byte)0x96, (byte)0xB2}); // Up Arrow
			byteMapping.put((byte) 0xAE, new byte[] {(byte)0xE2, (byte)0x96, (byte)0xBC}); // Down Arrow
			byteMapping.put((byte) 0xB2, new byte[] {(byte)0x36}); // 7 bars -> #6
			byteMapping.put((byte) 0xB3, new byte[] {(byte)0x35}); // 6 bars -> #5
			byteMapping.put((byte) 0xB4, new byte[] {(byte)0x34}); // 5 bars -> #4
			byteMapping.put((byte) 0xB5, new byte[] {(byte)0x33}); // 4 bars -> #3
			byteMapping.put((byte) 0xB6, new byte[] {(byte)0x32}); // 3 bars -> #2
			byteMapping.put((byte) 0xB7, new byte[] {(byte)0x31}); // 2 bars -> #1
			byteMapping.put((byte) 0xB8, new byte[] {(byte)0x30}); // 1 Bars -> #0
		}
		
	}
	
	/**
	 * Messages from the Navigation System to the Telephone
	 */
	class NavigationSystem extends IBusSystemCommand{
		
		private byte locationData = (byte) 0xA4;
		private byte gpsData = (byte) 0xA2;
		
		@SuppressLint("UseSparseArrays") 
		private Map<Byte, Method> IBusNavigationMap = new HashMap<Byte, Method>();
		
		public void parseGPSData(){
			// Parse out coordinate data
			List<String> coordData = new ArrayList<String>();
			for (int i = 6; i < currentMessage.size(); i++){
				// Convert to int to rid ourselves of preceding zeros. Yes, this is bad.
				coordData.add(
					String.valueOf(
						Integer.parseInt(
							bcdToStr(currentMessage.get(i))
						)
					)
				);
			}
			String latitudeNorthSouth = (String.format("%s", currentMessage.get(9)).charAt(1) == '0') ? "N" : "S";
			String longitudeEastWest = (String.format("%s", currentMessage.get(14)).charAt(1) == '0') ? "E" : "W";
			triggerCallback("onUpdateGPSCoordinates",
				String.format(
					"%s%s%s'%s\"%s %s%s%s'%s\"%s", 
					//Latitude
					coordData.get(0),
					(char) 0x00B0,
					(int) Math.round(Double.parseDouble(coordData.get(1)+coordData.get(2))),
					coordData.get(3).charAt(0),
					latitudeNorthSouth,
					//Longitude
					Integer.parseInt(coordData.get(4) + coordData.get(5)),
					(char) 0x00B0,
					(int) Math.round(Double.parseDouble(coordData.get(6)+coordData.get(7))),
					coordData.get(8).charAt(0),
					longitudeEastWest
				)
			);
		
			// Parse out altitude data which is in meters and is stored as a byte coded decimal
			int altitude = Integer.parseInt(
				bcdToStr(currentMessage.get(15)) + bcdToStr(currentMessage.get(16))
			);

			triggerCallback("onUpdateGPSAltitude", altitude);
			
			// Parse out time data which is in UTC
			String gpsUTCTime = String.format(
					"%s:%s:%s",
					currentMessage.get(18),
					currentMessage.get(19),
					currentMessage.get(20)
			);
			
			triggerCallback("onUpdateGPSTime", gpsUTCTime);
		}
				
		public void setLocale(){
			int lastData = 6;
			while(currentMessage.get(lastData) != (byte) 0x00){
				lastData++;
			}
			triggerCallback("onUpdateLocale", decodeMessage(currentMessage, 6, lastData-1));
		}
		
		public void setStreetLocation(){
			int lastData = 6;
			while(currentMessage.get(lastData) != (byte)0x3B){
				lastData++;
			}
			triggerCallback("onUpdateStreetLocation", decodeMessage(currentMessage, 6, lastData-1));
		}
		
		NavigationSystem(){
			try{
				IBusNavigationMap.put((byte)0x00, this.getClass().getMethod("parseGPSData"));
				IBusNavigationMap.put((byte)0x01, this.getClass().getMethod("setLocale"));
				IBusNavigationMap.put((byte)0x02, this.getClass().getMethod("setStreetLocation"));
			}catch(NoSuchMethodException e){
				// First world anarchy
			}
		}
		
		public void mapReceived(ArrayList<Byte> msg){
			currentMessage = msg;
			try{
				if(msg.get(3) == locationData || msg.get(3) == gpsData){
					IBusNavigationMap.get(msg.get(5)).invoke(this);
				}
			}catch(IllegalArgumentException | InvocationTargetException | IllegalAccessException e){
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Message from the MFL to the Telephone
	 */
	class SteeringWheelSystem extends IBusSystemCommand{

		public void mapReceived(ArrayList<Byte> msg) {
			currentMessage = msg;
			if(currentMessage.get(3) == 0x3B){
				if(currentMessage.get(4) == (byte) 0xA0) // Voice Btn
					triggerCallback("onVoiceBtnPress");
			}
		}
		
	}
	
	public TelephoneSystemCommand(){
		IBusDestinationSystems.put(DeviceAddressEnum.InstrumentClusterElectronics.toByte(), new IKESystem());
		IBusDestinationSystems.put(DeviceAddressEnum.MultiFunctionSteeringWheel.toByte(), new SteeringWheelSystem());
		IBusDestinationSystems.put(DeviceAddressEnum.NavigationEurope.toByte(), new NavigationSystem());
	}
	
}