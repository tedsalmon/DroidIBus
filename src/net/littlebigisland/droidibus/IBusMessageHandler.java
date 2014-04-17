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
	private IBusMessageListener mCallbackInterface = null;
	
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
				break;
			default:
				Log.d("DroidIBus", "Handling Unknown Message");
				break;
		}
	}
	
	// This is seriously bad. Maybe it's better now, we'll see
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
	
	private void handleRadio(ArrayList<Byte> msg){
		if (msg.get(2) == 0x3b && msg.get(3) == 0x23 && msg.get(4) == 0x62 && msg.get(5) == 0x10){
			Log.d("DroidIBus", "Handling Station Text");
			String str = decodeMessage(msg, 6, msg.size() - 1, (byte)0x20);
			Log.d("DroidIBus", String.format("Decoded Station Text '%s",str));
			if(mCallbackInterface != null){
				mCallbackInterface.onUpdateStation(str);
			}
		}
	}
	
	/**
	 * Register a callback listener
	 * @param cb
	 */
	public void registerCallbackListener(IBusMessageListener cb){
		Log.d("DroidIBus", "Registering Callback Listener");
		mCallbackInterface = cb;
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
				byte fByte = msg[0];
				String byte_str = "Decoded IBUS Message:";
				for(int i =0; i< msg.length; i++){
					byte_str =  String.format("%s 0x%02X", byte_str, msg[i]);
				}
				Log.d(TAG, byte_str);
				//debugText = byte_str + "\n" + debugText;
				switch(fByte){
					case 0x68:
						// Probably should have a verification function to get rid of this ugly shit?
						if ((msg[2] == 0x3b) && (msg[3] == 0x23) && (msg[4] == 0x62) && (msg[5] == 0x10)) {
							setStation(msg);
						}
						if(msg[2] == (byte) 0x3B){
							debugText = "Caught what might be tone data? " + byte_str + "\n" + debugText;
						}
						break;
					case (byte) 0x80:
						String IKEDATA = "";
						if(msg[3] == 0x19){
							IKEDATA = String.format("IKE: Outside: %s C Coolant: %s C", (int)msg[4], (int)msg[5]) + "\n" + debugText;
						}else if(msg[3] == 0x11){
							IKEDATA = String.format("IKE: Ignition state changed to %s", (int)msg[4]) + "\n" + debugText;
						}else if(msg[3] == 0x18){
							speedVal = String.format("%s km/h", (int)msg[4]* 2);
							rpmVal = String.format("%s", ((int)msg[5]* 100));
						}else if(msg[3] == 0x24){
							switch(msg[4]){
								case 0x01:
									//Time
									IKEDATA = String.format("IKE: Time %s", new String(Arrays.copyOfRange(msg, 6, msg.length-1), "UTF-8"));
									break;
								case 0x02:
									//Date
									IKEDATA = String.format("IKE: Date %s", new String(Arrays.copyOfRange(msg, 6, msg.length-1), "UTF-8"));
									break;
								case 0x03:
									// Outdoor Temperature
									outTempVal = new String(Arrays.copyOfRange(msg, 7, msg.length-1), "UTF-8");
									IKEDATA = String.format("IKE: Outdoor Temperature %s", outTempVal);
									break;
								case 0x04:
									//Fuel 1
									fuel1Val = new String(Arrays.copyOfRange(msg, 6, msg.length-1), "UTF-8");
									IKEDATA = String.format("IKE: Fuel 1 %s", fuel1Val);
									break;
								case 0x05:
									//Fuel 2
									fuel2Val = new String(Arrays.copyOfRange(msg, 6, msg.length-1), "UTF-8");
									IKEDATA = String.format("IKE: Fuel 2 %s", fuel2Val);
									break;
								case 0x06:
									//Range
									rangeVal = new String(Arrays.copyOfRange(msg, 6, msg.length-1), "UTF-8");
									IKEDATA = String.format("IKE: Range %s", rangeVal, "UTF-8");
									break;
								case 0x07:
									//Distance
									break;
								case 0x0A:
									// AVG Speed
									avgSpeedVal = new String(Arrays.copyOfRange(msg, 6, msg.length-1), "UTF-8");
									break;
							}
						}else{
							String items = "";
							for(int i = 2; i < msg.length; i++){
								items = items + String.format("%s ", (int)msg[i]);
							}
						}
						Log.d(TAG, IKEDATA);
						debugText = IKEDATA + "\n" + debugText;
						break;
					case (byte) 0xBF:
						String BROADCAST = "BROADCAST: ";
						for(int i = 2; i < msg.length; i++){
							BROADCAST = BROADCAST + String.format("%s ", (int)msg[i]);
						}
						Log.d(TAG, BROADCAST);
						debugText = BROADCAST + "\n" + debugText;
						break;
					case 0x3B:
						// Debug when the BM46 asks for data
						if(msg[2] == (byte) 0x80 && msg[3] == (byte)0x41){
							String system = "";
							switch(msg[4]){
								case 0x01:
									system = "Date";
									break;
								case 0x02:
									system = "Time";
									break;
								case 0x03:
									system = "Outdoor Temp";
									break;
								case 0x04:
									system = "Fuel 1";
									break;
								case 0x05:
									system = "Fuel 2";
									break;
								case 0x06:
									system = "Range";
									break;
								case 0x07:
									system = "Distance";
									break;
								case 0x08:
									system = "Unknown";
									break;
								case 0x09:
									system = "Limit";
									break;
								case 0x0A:
									system = "AVG Speed";;
									break;
								case 0x0E:
									system = "Timer";
									break;
								case 0x0F:
									system = "AUX Heater 1";
									break;
								case 0x10:
									system = "AUX Heater 2";
									break;
							}
							String obcReq = String.format("BM: IKE %s %s Request", system, (msg[msg.length - 1] == (byte)0x01) ? "Get" : "Set");
							debugText = debugText + "\n" + obcReq;
							Log.d(TAG, debugText);
						}
						break;
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
							Log.d(TAG, locationInfo);
							debugText = debugText + "\n" + locationInfo;
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
			
			// IBus helper functions below
			
			private void setStation(byte[] msg){
				int msgSize = msg.length - 2;
				while (msg[msgSize] == 0x20) {
					msgSize--;
				}
				byte[] displayBytes = new byte[msgSize - 5];
				for (int i = 0; i < displayBytes.length; i++) {
					displayBytes[i] = msg[i + 6];
				}
				try {
					stationText = new String(displayBytes, "UTF-8");
				} catch (Exception e) {
					Log.e(TAG, "Error encoding Station Data");
				}
			}
*/