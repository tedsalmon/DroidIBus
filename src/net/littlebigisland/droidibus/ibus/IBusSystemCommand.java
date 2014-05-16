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
	public IBusMessageHandler.MessageDecoder decodeMessage = null;
	
	public String decode(ArrayList<Byte> msg, int startByte, int endByte){
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
	
	abstract void mapReceived(ArrayList<Byte> msg);
}
