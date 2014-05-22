package net.littlebigisland.droidibus.ibus;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import android.util.Log;

/**
 * Handle messages emitted by the Radio unit
 */
class RadioSystemCommand extends IBusSystemCommand{
	
	/**
	 * Handle messages bound for the BoardMonitor from the Radio in the trunk
	 */
	class GFXNavigationSystem extends IBusSystemCommand{
		
		public void mapReceived(ArrayList<Byte> msg){
			currentMessage = msg;
			// This is the AM/FM text - Data starts after 0x41, which appears to be the
			// Index of the box to fill with this text on the BoardMonitor
			// 0x68 0x0B 0x3B 0xA5 0x62 0x01 0x41 0x20 0x46 0x4D 0x31 0x20 0xE5
			if(msg.get(3) == 0x23 && msg.get(4) == 0x62 && msg.get(5) == 0x10){
				stationText();
			}
		}
		
		private void stationText(){
			int msgSize = currentMessage.size() - 2;
			while (currentMessage.get(msgSize) == 0x20)
				msgSize--;
			byte[] displayBytes = new byte[msgSize - 5];
			for (int i = 0; i < displayBytes.length; i++)
				displayBytes[i] = currentMessage.get(i + 6);
			String str = "";
			try{
				str = new String(displayBytes, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			Log.d("DroidIBus", String.format("Handling Station Text - Got '%s'", str));
			if(mCallbackReceiver != null)
				mCallbackReceiver.onUpdateStation(str);
		}
		
	}
	
	/**
	 * Cstruct - Register destination systems
	 */
	RadioSystemCommand(){
		IBusDestinationSystems.put(DeviceAddress.GraphicsNavigationDriver.toByte(), new GFXNavigationSystem());
	}
}
