package net.littlebigisland.droidibus.ibus;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * This class represents the base of which all IBus Systems will extend from
 * in order to parse sent/received messages.
 * 
 * Subsequent subclasses implement message handlers for destination systems
 * using this same class. I.e. Source system IKE Extends IBusSystemCommand
 * it's child class, say GlobalBroadcastSystem also implements IBusSystemCommand
 */
public abstract class IBusSystemCommand {
	
	public IBusMessageReceiver mCallbackReceiver = null;
	
	public String bcdToStr(byte bcd) {
		StringBuffer strBuff = new StringBuffer();
		
		byte high = (byte) (bcd & 0xf0);
		high >>>= (byte) 4;	
		high = (byte) (high & 0x0f);
		byte low = (byte) (bcd & 0x0f);
		
		strBuff.append(high);
		strBuff.append(low);
		
		return strBuff.toString();
	}
	
	public String decodeMessage(ArrayList<Byte> msg, int startByte, int endByte){
		ArrayList<Byte> tempBytes = new ArrayList<Byte>();
		while(startByte <= endByte){
			tempBytes.add(msg.get(startByte));
			startByte++;
		}
		byte[] strByte = new byte[tempBytes.size()];
		for(int i = 0; i < tempBytes.size(); i++){
			strByte[i] = tempBytes.get(i);
		}
		try {
			return new String(strByte, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public void registerCallbacks(IBusMessageReceiver cb){
		mCallbackReceiver = cb;
	}	
	
	abstract void mapReceived(ArrayList<Byte> msg);
}
