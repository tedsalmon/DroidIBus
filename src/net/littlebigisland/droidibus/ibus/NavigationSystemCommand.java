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
		private Map<Byte, Method> IBusTelephoneMap = new HashMap<Byte, Method>();
		
		public void setGPSCoordinates(){
			List<String> strData = new ArrayList<String>();
			for (int i = 6; i < currentMessage.size(); i++) {
				// Convert to int to rid ourselves of preceeding zeros. Yes, this is bad.
				strData.add(
					String.valueOf(
						Integer.parseInt(
							bcdToStr(currentMessage.get(i))
						)
					)
				);
			}
			if(mCallbackReceiver != null)
				mCallbackReceiver.onUpdateGPSCoordinates(
					String.format(
						"%s%s%s'%s.%s\"%s %s%s%s'%s.%s\"%s", 
						//Latitude
						strData.get(0),
						(char) 0x00B0,
						strData.get(1), 
						strData.get(2),
						strData.get(3).charAt(0),
						(strData.get(3).charAt(1) == '0') ? "N" : "S",
						//Longitude
						Integer.parseInt(strData.get(4) + strData.get(5)),
						(char) 0x00B0,
						strData.get(6),
						strData.get(7),
						strData.get(8).charAt(0),
						(strData.get(8).charAt(1) == '0') ?"E" : "W"
					)
				);
		}
		
		public void setLocale(){
			int lastData = 6;
			while(currentMessage.get(lastData) != (byte) 0x00){
				lastData++;
			}
			if(mCallbackReceiver != null)
				mCallbackReceiver.onUpdateLocale(
					decodeMessage(currentMessage, 6, lastData)
				);
		}
		
		public void setStreetLocation(){
			int lastData = 6;
			while(currentMessage.get(lastData) != (byte)0x3B){
				lastData++;
			}
			if(mCallbackReceiver != null)
				mCallbackReceiver.onUpdateStreetLocation(
					decodeMessage(currentMessage, 6, lastData)
				);
		}
		
		Telephone(){
			try{
				IBusTelephoneMap.put((byte)0x00, this.getClass().getMethod("setGPSCoordinates", (Class<?>) null));
				IBusTelephoneMap.put((byte)0x01, this.getClass().getMethod("setLocale", (Class<?>) null));
				IBusTelephoneMap.put((byte)0x02, this.getClass().getMethod("setStreetLocation", (Class<?>) null));
			}catch(NoSuchMethodException e){
			}
		}
		
		public void mapReceived(ArrayList<Byte> msg){
			currentMessage = msg;
			try{
				if(msg.get(3) == locationData){
					IBusTelephoneMap.get(msg.get(5)).invoke(this, (Class<?>) null);
				}
			}catch(IllegalArgumentException e){
			}catch(InvocationTargetException e){
			}catch(IllegalAccessException e){
			}
		}
		
	}
	
	/**
	 * Cstruct - Register destination systems
	 */
	NavigationSystemCommand(){
		IBusDestinationSystems.put(DeviceAddress.Telephone.toByte(), new Telephone());
	}
}
