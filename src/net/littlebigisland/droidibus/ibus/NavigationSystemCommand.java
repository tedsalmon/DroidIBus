package net.littlebigisland.droidibus.ibus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavigationSystemCommand extends IBusSystemCommand {
	
	private Map<Byte, IBusSystemCommand> IBusNavMap = new HashMap<Byte, IBusSystemCommand>();
	
	class Telephone extends IBusSystemCommand{
		
		private byte locationData = (byte) 0xA4;
		private Map<Byte, Method> IBusTelephoneMap = new HashMap<Byte, Method>();
		
		public void setGPSCoordinates(){
			List<String> strData = new ArrayList<String>();
			for (int i = 0; i < currentMessage.size(); i++) {
				strData.add(bcdToStr(currentMessage.get(i)));
			}
			if(mCallbackReceiver != null)
				mCallbackReceiver.onUpdateGPSCoordinates(
					String.format(
						"Lat: %s %s' %s.%s\" Long: %s %s' %s\"", 
						strData.get(0), strData.get(1), strData.get(2),
						strData.get(3), strData.get(4) + strData.get(5),
						strData.get(6), strData.get(7), strData.get(8)
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
					new String("")//Arrays.copyOfRange(msg, 6, lastData), "UTF-8")
				);
		}
		
		public void setStreetLocation(){
			int lastData = 6;
			while(currentMessage.get(lastData) != (byte)0x3B){
				lastData++;
			}
			if(mCallbackReceiver != null)
				mCallbackReceiver.onUpdateStreetLocation(
					new String("")//Arrays.copyOfRange(msg, 6, lastData), "UTF-8")
				);
		}
		
		public void mapReceived(ArrayList<Byte> msg){
			currentMessage = msg;
			try{
				if(IBusTelephoneMap.isEmpty()){
					IBusTelephoneMap.put((byte)0x00, this.getClass().getMethod("setGPSCoordinates", (Class<?>) null));
					IBusTelephoneMap.put((byte)0x01, this.getClass().getMethod("setLocale", (Class<?>) null));
					IBusTelephoneMap.put((byte)0x02, this.getClass().getMethod("setStreetLocation", (Class<?>) null));
				}
				if(msg.get(3) == locationData){
					IBusTelephoneMap.get(msg.get(5)).invoke(this, (Class<?>) null);
				}
			}catch(IllegalArgumentException e){
			}catch(InvocationTargetException e){
			}catch(NoSuchMethodException e){
			}catch(IllegalAccessException e){
			}
		}
		
	}
	
	public void mapReceived(ArrayList<Byte> msg) {
		if(IBusNavMap.isEmpty()){
			IBusNavMap.put(DeviceAddress.Telephone.toByte(), new Telephone());
			// Register the callback listener here ;)
			for (Object key : IBusNavMap.keySet())
				IBusNavMap.get(key).registerCallbacks(mCallbackReceiver);
		}
		try{
			IBusNavMap.get((byte) msg.get(2)).mapReceived(msg);
		}catch(NullPointerException npe){
			// Things not in the map throw a NullPointerException
		}
	}
}
