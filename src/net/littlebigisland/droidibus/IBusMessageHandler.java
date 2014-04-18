package net.littlebigisland.droidibus;
/**
 * Message Parsing/Sending to IBus
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 */

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import android.util.Log;

/**
 * @author Ted S
 *
 */
public class IBusMessageHandler {
	// User Provided Callback interface implementation
	private IBusMessageReceiver mDataReceiver = null;
	// System constants
	public final byte IBUS_BodyModule = 0x00;
	public final byte IBUS_SunroofControl = 0x08;
	public final byte IBUS_CDChanger = 0x18;
	public final byte IBUS_RadioControlledClock = 0x28;
	public final byte IBUS_CheckControlModule = 0x30;
	public final byte IBUS_GraphicsNavigationDriver = 0x3B;
	public final byte IBUS_Diagnostic = 0x3F;
	public final byte IBUS_RemoteControlCentralLocking = 0x40;
	public final byte IBUS_GraphicsDriverRearScreen = 0x43;
	public final byte IBUS_Immobiliser = 0x44;
	public final byte IBUS_CentralInformationDisplay = 0x46;
	public final byte IBUS_MultiFunctionSteeringWheel = 0x50;
	public final byte IBUS_MirrorMemory = 0x51;
	public final byte IBUS_IntegratedHeatingAndAirConditioning = 0x5B;
	public final byte IBUS_ParkDistanceControl = 0x60;
	public final byte IBUS_Radio = 0x68;
	public final byte IBUS_DigitalSignalProcessingAudioAmplifier = 0x6A;
	public final byte IBUS_SeatMemory = 0x72;
	public final byte IBUS_SiriusRadio = 0x73;
	public final byte IBUS_CDChangerDINsize = 0x76;
	public final byte IBUS_NavigationEurope = 0x7F;
	public final byte IBUS_InstrumentClusterElectronics = (byte) 0x80;
	public final byte IBUS_MirrorMemorySecond = (byte) 0x9B;
	public final byte IBUS_MirrorMemoryThird = (byte) 0x9C;
	public final byte IBUS_RearMultiInfoDisplay = (byte) 0xA0;
	public final byte IBUS_AirBagModule = (byte) 0xA4;
	public final byte IBUS_SpeedRecognitionSystem = (byte) 0xB0;
	public final byte IBUS_NavigationJapan = (byte) 0xBB;
	public final byte IBUS_GlobalBroadcastAddress = (byte) 0xBF;
	public final byte IBUS_MultiInfoDisplay = (byte) 0xC0;
	public final byte IBUS_Telephone = (byte) 0xC8;
	public final byte IBUS_Assist = (byte) 0xCA;
	public final byte IBUS_LightControlModule = (byte) 0xD0;
	public final byte IBUS_SeatMemorySecond = (byte) 0xDA;
	public final byte IBUS_IntegratedRadioInformationSystem = (byte) 0xE0;
	public final byte IBUS_FrontDisplay = (byte) 0xE7;
	public final byte IBUS_RainLightSensor = (byte) 0xE8;
	public final byte IBUS_Television = (byte) 0xED;
	public final byte IBUS_OnBoardMonitor = (byte) 0xF0;
	public final byte IBUS_Broadcast = (byte) 0xFF;
	public final byte IBUS_Unset = (byte) 0x100;
	public final byte IBUS_Unknown = (byte) 0x101;
	
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
	 * @todo Implement correctly
	 * Temp 
	 * @param msg
	 */
	public void handleMessage(ArrayList<Byte> msg){
		// The first item in the IBus message indicates the source system
		switch(msg.get(0)){
			case IBUS_Radio:
				handleRadio(msg);
				break;
			case IBUS_InstrumentClusterElectronics:
				handleIKE(msg);
				break;
			case IBUS_GraphicsNavigationDriver:
				// 'Get' OBC Requests come from here
				break;
			default:
				Log.d("DroidIBus", "Handling Unknown Message");
				break;
		}
	}
	
	
	/**
	 * This method extracts bytes from an IBus Message and returns them as a UTF-8 String
	 * Helpful for most IKE/Location messages
	 * @TODO - Integrate with the above method. Method overloading is really ghetto for this use case
	 * 
	 * @param msg
	 * @param startByte
	 * @param endByte
	 * @return String	Decoded Bytes
	 */
	private String decodeMessage(ArrayList<Byte> msg, int startByte, int endByte, byte lastValidByte){
		Log.d("DroidIBus", "Decoding message");
		ArrayList<Byte> tempBytes = new ArrayList<Byte>();
		while(startByte <= endByte){
			byte tempByte = msg.get(startByte);
			if(tempByte != lastValidByte){
				tempBytes.add(msg.get(startByte));
			}
			startByte++;
		}
		byte[] strByte = new byte[tempBytes.size()];
		for(int i = 0; i < tempBytes.size(); i++){
			strByte[i] = tempBytes.get(i);
		}
		try {
			return new String(strByte, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	
	private String decodeMessage(ArrayList<Byte> msg, int startByte, int endByte){
		Log.d("DroidIBus", "Decoding message");
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	
	/**
	 * Handle messages received from the Radio subsystem
	 * 
	 */	
	private void handleRadio(ArrayList<Byte> msg){
		switch(msg.get(2)){
		
			case IBUS_GraphicsNavigationDriver:
				// Station Text
				if(msg.get(3) == 0x23 && msg.get(4) == 0x62 && msg.get(5) == 0x10){
					String str = decodeMessage(msg, 6, msg.size() - 1, (byte)0x20);
					Log.d("DroidIBus", String.format("Handling Station Text - Got '%s'", str));
					if(mDataReceiver != null) mDataReceiver.onUpdateStation(str);
				}
				break;
		}
	}
	
	/**
	 *  Handle messages received from IKE
	 */
	private void handleIKE(ArrayList<Byte> msg){
		switch(msg.get(3)){
			case 0x11:
				// Ignition State
				break;
			
			case 0x18:
				// Speed and RPM
				if(mDataReceiver != null)
					mDataReceiver.onUpdateSpeed(
						String.format("%s mph", ((int) msg.get(4) * 2) * 0.621371)
					);
					mDataReceiver.onUpdateRPM(
						String.format("%s", (int) msg.get(5) * 100)
					);		
				break;
			
			case 0x19:
				// Coolant Temperature
				if(mDataReceiver != null)
					mDataReceiver.onUpdateCoolantTemp(
						String.format("%s C", (int) msg.get(5))
					);
				break;
			
			case 0x24:
				// OBC 'Set' Data
				switch(msg.get(4)){
					case 0x01:
						// Time
						if(mDataReceiver != null)
							mDataReceiver.onUpdateTime(
								decodeMessage(msg, 6, msg.size() -1)
							);
						break;
					case 0x02:
						// Date
						if(mDataReceiver != null)
							mDataReceiver.onUpdateDate(
								decodeMessage(msg, 6, msg.size() -1)
							);
						break;
					case 0x03:
						// Outdoor Temperature
						if(mDataReceiver != null)
							mDataReceiver.onUpdateOutdoorTemp(
								decodeMessage(msg, 7, msg.size() -1)
							);
						break;
					case 0x04:
						// Fuel 1
						if(mDataReceiver != null)
							mDataReceiver.onUpdateFuel1(
								decodeMessage(msg, 6, msg.size() -1)
							);
						break;
					case 0x05:
						// Fuel 2
						if(mDataReceiver != null)
							mDataReceiver.onUpdateFuel2(
								decodeMessage(msg, 6, msg.size() -1)
							);
						break;
					case 0x06:
						// Range
						if(mDataReceiver != null)
							mDataReceiver.onUpdateRange(
								decodeMessage(msg, 6, msg.size() -1)
							);
						break;
					case 0x07:
						// Distance
						// Not currently working on implementing since we don't use on-board GPS maps
						break;
					case 0x08:
						// Unknown
						// Not implementing
						break;
					case 0x09:
						// Limit
						// Not implementing						
						break;
					case 0x0A:
						// AVG Speed
						if(mDataReceiver != null)
							mDataReceiver.onUpdateAvgSpeed(
								decodeMessage(msg, 6, msg.size() -1)
							);
						break;
					case 0x0E:
						// Timer
						// Not implementing
						break;
					case 0x0F:
						// AUX Heater 1
						// Not implementing
						break;
					case 0x10:
						// AUX Heater 2
						// Not implementing
						break;
				}
				break;
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