package net.littlebigisland.droidibus.ibus;
/**
 * IBusService
 * Communicate with the IBus using the IOIO
 * All Read/Writes are done here but message parsing and callbacks
 * are handled via the IBusMessenger class
 * 
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus.IBusService
 * 
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;


public class IBusMessageService extends IOIOService {

	private final IBinder mBinder = new IOIOBinder();
	@SuppressWarnings("unused")
	private Handler mHandler;
	private ArrayList<String> actionQueue = new ArrayList<String>();
	private String TAG = "DroidIBus";

	public IBusMessageHandler mIBusMessenger = new IBusMessageHandler();
	
	
	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 * @see ioio.lib.util.android.IOIOService#createIOIOLooper()
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new BaseIOIOLooper() {
			private Uart IBusConn;
			private InputStream busIn;
			@SuppressWarnings("unused")
			private OutputStream busOut;
			
			private DigitalOutput statusLED;
			private DigitalOutput faultPin;
			private DigitalOutput chipSelectPin;
			
			private int IBusRXPinId = 10;
			private int IBusTXPinId = 13;
			private int chipSelectPinId = 11;
			private int faultPinId = 12;
			
			private int msgLength;
			
			private Calendar time;
			private long lastRead;
			@SuppressWarnings("unused")
			private long lastSend;

			private ArrayList<Byte> readBuffer;
			
			private Map<Byte, IBusSystemCommand> IBusSysMap = new HashMap<Byte, IBusSystemCommand>();
			
			/**
			 * Called every time a connection with IOIO has been established.
			 * Setup the connection to the IBus and bring up the CS/Fault Pins on the MCP2004
			 * 
			 * @throws ConnectionLostException
			 *             When IOIO connection is lost.
			 * 
			 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
			 */
			@Override
			protected void setup() throws ConnectionLostException, InterruptedException {
				Log.d(TAG, "Running IOIO Setup");
				IBusConn = ioio_.openUart(
					IBusRXPinId, IBusTXPinId, 9600, Uart.Parity.EVEN, Uart.StopBits.ONE
				);
				
				statusLED = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
				/* Set these HIGH per the MCP2004 data sheet. 
				 * Not required so long as you put a 270ohm resistor from 5V to pin 2 */
				chipSelectPin = ioio_.openDigitalOutput(chipSelectPinId, true);
				chipSelectPin.write(true);
				faultPin = ioio_.openDigitalOutput(faultPinId, true);
				faultPin.write(true);
				
				busIn = IBusConn.getInputStream();
				busOut = IBusConn.getOutputStream();
				
				// Initiate required values
				readBuffer = new ArrayList<Byte>();
				msgLength = 0;
				
				// Timeout stuff 
				time = Calendar.getInstance();
				lastRead = time.getTimeInMillis();
				lastSend = time.getTimeInMillis();
			}
			
			/**
			 * Called repetitively while the IOIO is connected.
			 * Reads and writes to the IBus
			 * @throws ConnectionLostException
			 *             When IOIO connection is lost.
			 * 
			 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
			 */
			@Override
			public void loop() throws ConnectionLostException, InterruptedException {
				/*
				 * This is the main logic loop where we communicate with the IBus
				 */
				statusLED.write(true);
				// Timeout the buffer if we don't get data for 30ms
				if ((Calendar.getInstance().getTimeInMillis() - lastRead) > 30) {
					readBuffer.clear();
				}
				try {
					/* Handle incoming IBus data.
					 * Read incoming bytes into readBuffer.
					 * Skip if there's nothing to read.
					 */
					if (busIn.available() > 0) {
						lastRead = Calendar.getInstance().getTimeInMillis();
						readBuffer.add((byte) busIn.read());
						/* Set message size to a large number (256) if we haven't gotten the message
						 * length from the second byte of the IBus Message, else set message length to 
						 * the length provided by IBus.
						 */
						if (readBuffer.size() == 1) {
							msgLength = 256;
						} else if (readBuffer.size() == 2) {
							msgLength = (int) readBuffer.get(1);
						}
						// Read until readBuffer contains msgLength plus two more bytes for the full message
						if (readBuffer.size() == msgLength + 2) {
							if(checksumMessage(readBuffer)) {
								Log.d(TAG, "Handling Bytes");
								handleMessage(readBuffer);
							}
							readBuffer.clear();
						}
					}else if(actionQueue.size() > 0){
						lastSend = Calendar.getInstance().getTimeInMillis();
					}
				} catch (IOException e) {
					Log.e(TAG, String.format("IOIO IOException [%s] in IBusService.loop()", e.getMessage()));
				}
				statusLED.write(false);
				Thread.sleep(2);
			}
				
			/**
			 * Verify that the IBus Message is legitimate 
			 * by XORing all bytes if correct, the product 
			 * should be 0x00
			 * 
			 * @param ArrayList<byte> msgBuffer	The buffer containing all bytes in the Message
			 * @return boolean	 true if the message isn't corrupt, otherwise false
			 */
			private boolean checksumMessage(ArrayList<Byte> msgBuffer) {
				byte cksum = 0x00;
				for(byte msg : msgBuffer){
					cksum = (byte) (cksum ^ msg);
				}
				return (cksum == 0x00) ? true : false;
			}
				
			/**
			 * Send the inbound message to the correct Handler 
			 * @param msg
			 */
			private void handleMessage(ArrayList<Byte> msg){
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
		};
	}
	/**
	 * Add an action into the queue of message waiting to be sent
	 * @param act	ENUM String of action to be performed
	 */
	public void addAction(String act){
		actionQueue.add(act);
	}
	
	public void setCallbackListener(IBusMessageReceiver listener){
		mIBusMessenger.registerCallbackListener(listener);
	}
	
	public void disable(){
		stopSelf();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Call super.onStart because it starts the IOIOAndroidApplicationHelper 
		// and super.onStartCommand is not implemented
		super.onStart(intent, startId);
		handleStartup(intent);
		return START_STICKY;
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		handleStartup(intent);
	}	

	private void handleStartup(Intent intent) {
		mHandler = new Handler();
	}
	
	/** 
	 * A class to create our IOIO service.
	 */
    public class IOIOBinder extends Binder {
    	public IBusMessageService getService() {
            return IBusMessageService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}