package net.littlebigisland.droidibus.ibus;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the base of which all IBus Systems will extend from
 * in order to parse sent/received messages.
 * 
 * Subsequent subclasses implement message handlers for destination systems
 * using this same class. I.e. Source system IKE Extends IBusSystemCommand
 * it's child class, say GlobalBroadcastSystem also implements IBusSystemCommand.
 * This represents an IBus Message's "Source" and "Destination" system architecture.
 * 
 * Please note that it makes the most sense to do received message processing inside the
 * destination implementation HOWEVER, the source system should be the one handling
 * out bound messages to another destination system. For example, BM -> IKE Messages
 * should be initiated from the BM Class, not the IKE class nor a child
 * implementation of the IKE class inside the BM class.
 */
public abstract class IBusSystemCommand {
	
	// The variable that holds an instances of the implementors callback structure
	public IBusMessageReceiver mCallbackReceiver = null;
	// ArrayList holding the message currently being processed.
	public ArrayList<Byte> currentMessage = null;
	// Map used to map implementation of Destination systems from each Source System 
	public Map<Byte, IBusSystemCommand> IBusDestinationSystems = new HashMap<Byte, IBusSystemCommand>();
	
	/** 
	 * Converts a Byte Coded Decimal to it's String representation
	 * @param bcd The byte to decode
	 * @return 	  The String representation of the param byte
	 */
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
	
	/** 
	 * Decode Parts of an IBus Message from Bytes into a readable String
	 * 
	 * @param msg 		The Message we're decoding
	 * @param startByte	The index of the first byte we are extracting
	 * @param endByte	The index of the last byte we are extracting
	 * @return The message in UTF-8 String formatting
	 */
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
	
	/** 
	 * Use the implementations destination system Map to lookup
	 * the instance class that handles the given message. In some instances
	 * it makes to override this method with more implementation specific code.
	 * 
	 * @param msg	The Message to be handled
	 */
	public void mapReceived(ArrayList<Byte> msg){
		try{
			IBusDestinationSystems.get((byte) msg.get(2)).mapReceived(msg);
		}catch(NullPointerException npe){
			// Things not in the map throw a NullPointerException
		}
	}
	
	/**
	 * Register the programmers callback implementation with the class.
	 * Introspectively register this receiver to all children in the
	 * `IBusDestinationSystems` Map.
	 * 
	 * @param cb 	Your implementation of the IBusMessageReceiver
	 * 
	 */
	public void registerCallbacks(IBusMessageReceiver cb){
		mCallbackReceiver = cb;
		for (Object key : IBusDestinationSystems.keySet())
			IBusDestinationSystems.get(key).registerCallbacks(mCallbackReceiver);
	}
}
