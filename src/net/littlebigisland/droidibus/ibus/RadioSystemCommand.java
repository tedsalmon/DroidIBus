package net.littlebigisland.droidibus.ibus;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

/**
 * Handle messages emitted by the Radio unit
 */
class RadioSystemCommand extends IBusSystemCommand{
	private Map<Byte, IBusSystemCommand> IBusRadioMap = new HashMap<Byte, IBusSystemCommand>();
	
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
			if(mCallbackReceiver != null) mCallbackReceiver.onUpdateStation(str);
		}
		
	}
	
	public void mapReceived(ArrayList<Byte> msg){
		// private byte[] modeBtnPress = new byte[] {(byte)0xF0, 0x04, 0x68, 0x48, 0x23,(byte) 0xF7};
		// private byte[] modeBtnRls = new byte[] {(byte)0xF0, 0x04, 0x68, 0x48, 0x23,(byte) 0x77};
		if(IBusRadioMap.isEmpty()){
			IBusRadioMap.put(DeviceAddress.GraphicsNavigationDriver.toByte(), new GFXNavigationSystem());
			// Register the callback listener here ;)
			for (Object key : IBusRadioMap.keySet())
				IBusRadioMap.get(key).registerCallbacks(mCallbackReceiver);
		}
		// The first item in the IBus message indicates the source system
		try{
			IBusRadioMap.get((byte) msg.get(2)).mapReceived(msg);
		}catch(NullPointerException npe){
			// Things not in the map throw a NullPointerException
		}
	}
}
