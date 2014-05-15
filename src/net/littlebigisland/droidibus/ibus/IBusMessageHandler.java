package net.littlebigisland.droidibus.ibus;
/**
 * Message Parsing/Sending to IBus
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 */

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class IBusMessageHandler {
	// User Provided Callback interface implementation
	public IBusMessageReceiver mDataReceiver = null;
	
	private Map<Byte, IBusSystemCommand> IBusSysMap = new HashMap<Byte, IBusSystemCommand>();
	
	/**
	 * Don't argue, this belongs here.
	 *
	 */
	public class MessageDecoder{
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
	}
	
	/**
	 * Verify that the IBus Message is legitimate 
	 * by XORing all bytes if correct, the product 
	 * should be 0x00
	 * 
	 * @param ArrayList<byte> msgBuffer	The buffer containing all bytes in the Message
	 * @return boolean	 true if the message isn't corrupt, otherwise false
	 */
	public boolean checksumMessage(ArrayList<Byte> msgBuffer) {
		byte cksum = 0x00;
		for(byte msg : msgBuffer){
			cksum = (byte) (cksum ^ msg);
		}
		return (cksum == 0x00) ? true : false;
	}
	
	/**
	 * Temp 
	 * @param msg
	 */
	public void handleMessage(ArrayList<Byte> msg){
		// This may be repeatedly executed but in the interest of readability
		// I'm not going to move it back into the class construct
		if(IBusSysMap.isEmpty()){
			IBusSysMap.put(DeviceAddress.Radio.toByte(), new RadioSystemCommand());
			IBusSysMap.put(DeviceAddress.InstrumentClusterElectronics.toByte(), new IKESystemCommand());
		}
		// The first item in the IBus message indicates the source system
		try{
			IBusSysMap.get(msg.get(0)).mapReceived(msg);
		}catch(NullPointerException npe){
			// Things not in the map throw a NullPointerException
		}
	}
	
	/**
	 * Register a callback listener
	 * @param cb
	 */
	public void registerCallbackListener(IBusMessageReceiver cb){
		mDataReceiver = cb;
	}
}

//Messages
/*
private byte[] volUp = new byte[] {0x50, 0x04, 0x68, 0x32, 0x11, 0x1F};
private byte[] volDown = new byte[] {0x50, 0x04, 0x68, 0x32, 0x10, 0x1E};
private byte[] nextBtnPress = new byte[] {0x50, 0x04, 0x68, 0x3B, 0x01, 0x06};
private byte[] nextBtnRls = new byte[] {0x50, 0x04, 0x68, 0x3B, 0x21, 0x26};
private byte[] prevBtnPress = new byte[] {0x50, 0x04, 0x68, 0x3B, 0x08, 0x0F};
private byte[] prevBtnRls = new byte[] {0x50, 0x04, 0x68, 0x3B, 0x28, 0x2f};
private byte[] modeBtnPress = new byte[] {(byte)0xF0, 0x04, 0x68, 0x48, 0x23,(byte) 0xF7};
private byte[] modeBtnRls = new byte[] {(byte)0xF0, 0x04, 0x68, 0x48, 0x23,(byte) 0x77};
private byte[] fuel2Rqst = new byte[] {0x3B, 0x05, (byte)0x80, 0x41, 0x05, 0x01, (byte)0xFB};
*/
/*
			public void handleMessage(byte[] msg) throws IOException{
					case 0x7F:
						if(msg[3] == (byte)0xA4){
							String locationInfo = "";
							int lastData = 0;
							switch(msg[5]){
								case 0x00:
									locationInfo = "GPS: Coordinates: " + getGPSCoords(Arrays.copyOfRange(msg, 6, 15));
									break;
								case 0x01:
									lastData = 6;
									while(msg[lastData] != (byte) 0x00){
										lastData++;
									}
									locationInfo = String.format("GPS: Locale: %s", new String(Arrays.copyOfRange(msg, 6, lastData), "UTF-8"));
									break;
								case 0x02:
									lastData = 6;
									while(msg[lastData] != (byte) 0x3B){
										lastData++;
									}
									locationInfo = String.format("GPS: Street: %s", new String(Arrays.copyOfRange(msg, 6, lastData), "UTF-8"));
									break;
							}
						}
						break;
				}
			}
			
			public String bcdToStr(byte bcd) {
				StringBuffer strBuff = new StringBuffer();
				
				byte high = (byte) (bcd & 0xf0);
				high >>>= (byte) 4;	
				high = (byte) (high & 0x0f);
				byte low = (byte) (bcd & 0x0f);
				
				strBuff.append(high);
				strBuff.append(low);
				
				return sb.toString();
			}
			
			private String getGPSCoords(byte[] coordData){
				List<String> strData = new ArrayList<String>();
				for (int i = 0; i < coordData.length; i++) {
					strData.add(bcdToStr(coordData[i]));
				}
				return String.format(
						"Lat: %s¼ %s' %s.%s\" Long: %s¼ %s' %s\"", 
						strData.get(0), strData.get(1), strData.get(2),
						strData.get(3), strData.get(4) + strData.get(5),
						strData.get(6), strData.get(7), strData.get(8)
				);
			}
			
			private void sendMessage(byte[] msg) throws IOException{
				for(int i = 0; i < msg.length; i++){
					busOut.write(msg[i]);
					byte_str =  String.format("%s 0x%02X", byte_str, msg[i]);
				}
			}
*/
