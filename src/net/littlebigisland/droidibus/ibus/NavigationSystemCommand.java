package net.littlebigisland.droidibus.ibus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 
 * Handle messages from the Navi System
 */
public class NavigationSystemCommand extends IBusSystemCommand {
	
	/**
	 * Handle messages bound for the Telephone system
	 * This actually contains our GPS data
	 */
	class Telephone extends IBusSystemCommand{
		
		private byte locationData = (byte) 0xA4;
		private byte gpsData = (byte) 0xA2;
		private Map<Byte, Method> IBusTelephoneMap = new HashMap<Byte, Method>();
		
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
		
		Telephone(){
			try{
				IBusTelephoneMap.put((byte)0x00, this.getClass().getMethod("parseGPSData"));
				IBusTelephoneMap.put((byte)0x01, this.getClass().getMethod("setLocale"));
				IBusTelephoneMap.put((byte)0x02, this.getClass().getMethod("setStreetLocation"));
			}catch(NoSuchMethodException e){
				// First world anarchy
			}
		}
		
		public void mapReceived(ArrayList<Byte> msg){
			currentMessage = msg;
			try{
				if(msg.get(3) == locationData || msg.get(3) == gpsData){
					IBusTelephoneMap.get(msg.get(5)).invoke(this);
				}
			}catch(IllegalArgumentException | InvocationTargetException | IllegalAccessException e){
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Cstruct - Register destination systems
	 */
	NavigationSystemCommand(){
		IBusDestinationSystems.put(DeviceAddressEnum.Telephone.toByte(), new Telephone());
	}
}
