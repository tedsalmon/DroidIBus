package net.littlebigisland.droidibus.ibus;
/**
 * IBusService
 * Communicate with the IBus using the IOIO
 * All Read/Writes are done here
 * 
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus.ibus.IBusMessageService
 * 
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
	private ArrayList<IBusCommands> actionQueue = new ArrayList<IBusCommands>();
	private String TAG = "DroidIBus";
	private IBusMessageReceiver mIBusCbListener = null;
	private Map<Byte, String> DeviceLookup = new HashMap<Byte, String>(); // For logging
	
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
			private boolean callbackRegistered = false;
			
			private Uart IBusConn;
			private InputStream busIn;
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
			private long lastSend;

			private ArrayList<Byte> readBuffer;
			
			private Map<Byte, IBusSystemCommand> IBusSysMap = new HashMap<Byte, IBusSystemCommand>();
			
			private Map<IBusCommands, IBusMethodHolder> IBusCommandMap = new HashMap<IBusCommands, IBusMethodHolder>();
			
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
				// If we have callbacks setup we can start handling messages
				if(!callbackRegistered && mIBusCbListener != null)
					initiateHandlers();
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
								String data = "";
								for(int i = 0; i<readBuffer.size(); i++)
									data = String.format("%s%02X ", data, readBuffer.get(i));
								Log.d(TAG, String.format(
									"Received Message (%s -> %s): %s",
									DeviceLookup.get(readBuffer.get(0)),
									DeviceLookup.get(readBuffer.get(2)),
									data
								));
								handleMessage(readBuffer);
							}
							readBuffer.clear();
						}
					}else if(actionQueue.size() > 0){
						// Wait at least 75ms between messages and then write out to the bus
						if ((Calendar.getInstance().getTimeInMillis() - lastSend) > 75) {
							Log.d(TAG, String.format("Sending %s Command out", actionQueue.get(0).toString()));
							IBusMethodHolder command = IBusCommandMap.get(actionQueue.get(0));
							byte[] outboundMsg = new byte[] {};
							try {
								outboundMsg = (byte[]) command.methodReference.invoke(command.classInstance);
							} catch (IllegalAccessException e) {
								e.printStackTrace();
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							} catch (InvocationTargetException e) {
								e.printStackTrace();
							}
							actionQueue.remove(0);
							// Write the message out to the bus byte by byte
							String out = "Sending: ";
							for(int i = 0; i < outboundMsg.length; i++){
								out = String.format("%s %02X", out, outboundMsg[i]);
								busOut.write(outboundMsg[i]);
							}
							Log.d(TAG, out);
							lastSend = Calendar.getInstance().getTimeInMillis();
							
						}
					}
				} catch (IOException e) {
					Log.e(TAG, String.format("IOIO IOException [%s] in IBusService.loop()", e.getMessage()));
				}
				statusLED.write(false);
				Thread.sleep(2);
			}
			
			/**
			 * This class is used to hold a class instance, method instance
			 * and any possible optional arguments. These instances will be placed into
			 * a map and called when a command execution request comes into the `actionQueue`
			 *
			 */
			class IBusMethodHolder{
				
				public IBusSystemCommand classInstance = null;
				public Method methodReference = null;
				
				IBusMethodHolder(IBusSystemCommand cls, Method mth){
					classInstance = cls;
					methodReference = mth;
				}
				
			}
			
			private void initiateHandlers(){
				// Map Device source Addresses with the respective handler class
				IBusSysMap.put(DeviceAddress.Radio.toByte(), new RadioSystemCommand());
				IBusSysMap.put(DeviceAddress.InstrumentClusterElectronics.toByte(), new IKESystemCommand());
				IBusSysMap.put(DeviceAddress.NavigationEurope.toByte(), new NavigationSystemCommand());
				IBusSysMap.put(DeviceAddress.MultiFunctionSteeringWheel.toByte(), new SteeringWheelSystemCommand());
				IBusSysMap.put(DeviceAddress.GraphicsNavigationDriver.toByte(), new BoardMonitorSystemCommand());
				// Register the callback listener here ;)
				for (Object key : IBusSysMap.keySet())
					IBusSysMap.get(key).registerCallbacks(mIBusCbListener);
				
				// Register functions
				IBusSystemCommand BM = IBusSysMap.get(DeviceAddress.GraphicsNavigationDriver.toByte());
				try {
					IBusCommandMap.put(IBusCommands.BMToIKEGetTime, new IBusMethodHolder(BM, BM.getClass().getMethod("getTime")));
					IBusCommandMap.put(IBusCommands.BMToIKEGetDate, new IBusMethodHolder(BM, BM.getClass().getMethod("getDate")));
					IBusCommandMap.put(IBusCommands.BMToIKEGetOutdoorTemp, new IBusMethodHolder(BM, BM.getClass().getMethod("getOutdoorTemp")));
					IBusCommandMap.put(IBusCommands.BMToIKEGetFuel1, new IBusMethodHolder(BM, BM.getClass().getMethod("getFuel1")));
					IBusCommandMap.put(IBusCommands.BMToIKEGetFuel2, new IBusMethodHolder(BM, BM.getClass().getMethod("getFuel2")));
					IBusCommandMap.put(IBusCommands.BMToIKEGetRange, new IBusMethodHolder(BM, BM.getClass().getMethod("getRange")));
					IBusCommandMap.put(IBusCommands.BMToIKEGetAvgSpeed, new IBusMethodHolder(BM, BM.getClass().getMethod("getAvgSpeed")));
					IBusCommandMap.put(IBusCommands.BMToRadioGetStatus, new IBusMethodHolder(BM, BM.getClass().getMethod("getRadioStatus")));
					
					IBusCommandMap.put(IBusCommands.BMToIKEResetFuel1, new IBusMethodHolder(BM, BM.getClass().getMethod("resetFuel1")));
					IBusCommandMap.put(IBusCommands.BMToIKEResetFuel2, new IBusMethodHolder(BM, BM.getClass().getMethod("resetFuel2")));
					IBusCommandMap.put(IBusCommands.BMToIKEResetAvgSpeed, new IBusMethodHolder(BM, BM.getClass().getMethod("resetAvgSpeed")));
					
					IBusCommandMap.put(IBusCommands.BMToRadioModePress, new IBusMethodHolder(BM, BM.getClass().getMethod("sendModePress")));
					IBusCommandMap.put(IBusCommands.BMToRadioModeRelease, new IBusMethodHolder(BM, BM.getClass().getMethod("sendModeRelease")));
					IBusCommandMap.put(IBusCommands.BMToRadioVolumeUp, new IBusMethodHolder(BM, BM.getClass().getMethod("sendVolumeUp")));
					IBusCommandMap.put(IBusCommands.BMToRadioVolumeDown, new IBusMethodHolder(BM, BM.getClass().getMethod("sendVolumeDown")));
					
					IBusCommandMap.put(IBusCommands.BMToRadioTuneFwdPress, new IBusMethodHolder(BM, BM.getClass().getMethod("sendSeekFwdPress")));
					IBusCommandMap.put(IBusCommands.BMToRadioTuneFwdRelease, new IBusMethodHolder(BM, BM.getClass().getMethod("sendSeekFwdRelease")));
					IBusCommandMap.put(IBusCommands.BMToRadioTuneRevPress, new IBusMethodHolder(BM, BM.getClass().getMethod("sendSeekRevPress")));
					IBusCommandMap.put(IBusCommands.BMToRadioTuneRevRelease, new IBusMethodHolder(BM, BM.getClass().getMethod("sendSeekRevRelease")));
					
					IBusCommandMap.put(IBusCommands.BMToRadioFMPress, new IBusMethodHolder(BM, BM.getClass().getMethod("sendFMPress")));
					IBusCommandMap.put(IBusCommands.BMToRadioFMRelease, new IBusMethodHolder(BM, BM.getClass().getMethod("sendFMRelease")));
					IBusCommandMap.put(IBusCommands.BMToRadioAMPress, new IBusMethodHolder(BM, BM.getClass().getMethod("sendAMPress")));
					IBusCommandMap.put(IBusCommands.BMToRadioAMRelease, new IBusMethodHolder(BM, BM.getClass().getMethod("sendAMRelease")));
					
					IBusCommandMap.put(IBusCommands.BMToRadioInfoPress, new IBusMethodHolder(BM, BM.getClass().getMethod("sendInfoPress")));
					IBusCommandMap.put(IBusCommands.BMToRadioInfoRelease, new IBusMethodHolder(BM, BM.getClass().getMethod("sendInfoRelease")));
				} catch (NoSuchMethodException e) {
					Log.e(TAG, e.getMessage());
				}
				callbackRegistered = true;
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
				// The first item in the IBus message indicates the source system
				try{
					IBusSysMap.get(msg.get(0)).mapReceived(msg);
				}catch(NullPointerException npe){
					// Things not in the map throw a NullPointerException
				}
			}
		};
	}
	
	/**
	 * Add an action into the queue of message waiting to be sent
	 * @param cmd	ENUM Action to be performed
	 */
	public void sendCommand(IBusCommands cmd){
		actionQueue.add(cmd);
	}
	
	/**
	 * Add an action into the queue of message waiting to be sent
	 * @param cmd	ENUM Action to be performed
	 * @param param	The data to send to the method
	 */
	public void sendCommand(IBusCommands cmd, String param){
		actionQueue.add(cmd);
	}
	
	public void setCallbackListener(IBusMessageReceiver listener){
		mIBusCbListener = listener;
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

	/**
	 * Take care of tasks to be done on every start up
	 * @param intent Intent from onStart/onStartCommand
	 */
	private void handleStartup(Intent intent) {
		mHandler = new Handler();
		for(DeviceAddress d : DeviceAddress.values())
			DeviceLookup.put(d.toByte(), d.name());
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		Log.d(TAG, "IBusMessageService stopping");
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