package net.littlebigisland.droidibus.ibus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;

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
					decodeMessage(currentMessage, 6, lastData-1)
				);
		}
		
		public void setStreetLocation(){
			int lastData = 6;
			while(currentMessage.get(lastData) != (byte)0x3B){
				lastData++;
			}
			if(mCallbackReceiver != null)
				mCallbackReceiver.onUpdateStreetLocation(
					decodeMessage(currentMessage, 6, lastData-1)
				);
		}
		
		Telephone(){
			try{
				IBusTelephoneMap.put((byte)0x00, this.getClass().getMethod("setGPSCoordinates"));
				IBusTelephoneMap.put((byte)0x01, this.getClass().getMethod("setLocale"));
				IBusTelephoneMap.put((byte)0x02, this.getClass().getMethod("setStreetLocation"));
			}catch(NoSuchMethodException e){
				// First world anarchy
			}
		}
		
		public void mapReceived(ArrayList<Byte> msg){
			Log.d("DroidIBus", "Got some kind of Navi data");
			currentMessage = msg;
			try{
				if(msg.get(3) == locationData || msg.get(3) == gpsData){
					Log.d("DroidIBus", "Invoking method for navi data!");
					IBusTelephoneMap.get(msg.get(5)).invoke(this);
				}
			}catch(IllegalArgumentException e){
				Log.d("DroidIBus", "Illegal Argument to Navi Method");
			}catch(InvocationTargetException e){
				Log.d("DroidIBus", "Invocation Target Exception to Navi Method");
			}catch(IllegalAccessException e){
				Log.d("DroidIBus", "Illegal Access to Navi Method");
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
