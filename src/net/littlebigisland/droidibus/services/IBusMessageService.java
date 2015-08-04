package net.littlebigisland.droidibus.services;
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

import net.littlebigisland.droidibus.ibus.IBusCommand;
import net.littlebigisland.droidibus.ibus.IBusSystem;
import net.littlebigisland.droidibus.ibus.systems.BoardMonitorSystem;
import net.littlebigisland.droidibus.ibus.systems.BroadcastSystem;
import net.littlebigisland.droidibus.ibus.systems.FrontDisplay;
import net.littlebigisland.droidibus.ibus.systems.GlobalBroadcastSystem;
import net.littlebigisland.droidibus.ibus.systems.GFXNavigationSystem;
import net.littlebigisland.droidibus.ibus.systems.IKESystem;
import net.littlebigisland.droidibus.ibus.systems.RadioSystem;
import net.littlebigisland.droidibus.ibus.systems.SteeringWheelSystem;
import net.littlebigisland.droidibus.ibus.systems.TelephoneSystem;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

@SuppressLint("UseSparseArrays")
public class IBusMessageService extends IOIOService{
    
    private final static String TAG = "DroidIBus::IBusMessageService";
    
    private final IBinder mBinder = new IOIOBinder();
    private ArrayList<IBusCommand> mCommandQueue = new ArrayList<IBusCommand>();
    
    private Map<Byte, IBusSystem> IBusSysMap = new HashMap<Byte, IBusSystem>();

    private Map<Byte, String> mDeviceLookup = new HashMap<Byte, String>();
    private boolean mIsIOIOConnected = false;
    private int mClientsConnected = 0;
    
    
    public class IOIOBinder extends Binder{
        public IBusMessageService getService(){
            return IBusMessageService.this;
        }
    }
    
    /**
     * This is the thread on which all the IOIO activity happens. The
     * setup() method will be called right after a connection with the IOIO has
     * been established, which might happen several times! After loop() will
     * be called repetitively until the IOIO is disconnected.
     */
    @Override
    protected IOIOLooper createIOIOLooper(){
        return new BaseIOIOLooper(){
            
            private Uart IBusConn;
            private InputStream busIn;
            private OutputStream busOut;
            
            private DigitalOutput statusLED;
            private DigitalOutput faultPin;
            private DigitalOutput chipSelectPin;
            
            private static final int IBUS_RX_PIN = 10;
            private static final int IBUS_TX_PIN = 13;
            private static final int IBUS_CS_PIN = 11;
            private static final int IBUS_ER_PIN = 12;
            
            // Set the IBus message protocol byte indexes
            private static final int MSG_SRC_IDX = 0;
            private static final int MSG_LEN_IDX = 1;
            private static final int MSG_DEST_IDX = 2;
            
            // Settings in milliseconds
            private static final int BUFFER_TIMEOUT = 75;
            private static final int SEND_WAIT = 100;

            private int msgLength = 0;
            
            private long lastRead;
            private long lastSend;

            private ArrayList<Byte> readBuffer = new ArrayList<Byte>();
            
            /**
             * Called every time a connection with IOIO has been established.
             * Setup the connection to the IBus using the MCP2004
             * 
             * @throws ConnectionLostException When IOIO connection is lost.
             */
            @Override
            protected void setup() throws ConnectionLostException, InterruptedException{
                Log.d(TAG, "IOIO Setup");
                IBusConn = ioio_.openUart(
                    IBUS_RX_PIN, IBUS_TX_PIN, 9600, 
                    Uart.Parity.EVEN, Uart.StopBits.ONE
                );
                statusLED = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
                /* Set these HIGH per the MCP2004 data sheet. 
                 * Not required so long as you put a 10k 
                 * resistor from 3.3V to pin 2 */
                chipSelectPin = ioio_.openDigitalOutput(IBUS_CS_PIN, true);
                chipSelectPin.write(true);
                
                faultPin = ioio_.openDigitalOutput(IBUS_ER_PIN, true);
                faultPin.write(true);
                
                busIn = IBusConn.getInputStream();
                busOut = IBusConn.getOutputStream();
                
                lastRead = getTime();
                // Add 250ms to prevent bus spam 
                lastSend = getTime() + 250;
                mIsIOIOConnected = true;
            }
            
            /**
             * Called repetitively while the IOIO is connected.
             * Reads and writes to the IBus
             * 
             * @throws ConnectionLostException When IOIO connection is lost.
             * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
             */
            @Override
            public void loop() throws ConnectionLostException, InterruptedException{
                long timeNow = getTime();
                long lastCmdSent = timeNow - lastSend;
                long lastCmdRead = timeNow - lastRead;
                // Clear the buffer if we don't get data for 75ms
                if(lastCmdRead >= BUFFER_TIMEOUT && !readBuffer.isEmpty()){
                    logBufferError("Buffer Timeout", readBuffer);
                    readBuffer.clear();
                }
                try{
                    // Read into buffer when bytes are available
                    if(busIn.available() > 0){
                        statusLED.write(true);
                        lastRead = getTime();
                        readBuffer.add((byte) busIn.read());

                        if(readBuffer.size() > 1){
                            if(readBuffer.size() == 2){
                                msgLength = (int)readBuffer.get(MSG_LEN_IDX);
                                if(msgLength <= 0){
                                    Log.e(TAG, "Invalid buffer size: " + msgLength);
                                    readBuffer.clear();
                                }
                            }
                            if(readBuffer.size() > msgLength + 2){
                                logBufferError("Buffer too large", readBuffer);
                                readBuffer.clear();
                            }
                            // Read until buffer size equals the message length
                            if(readBuffer.size() == msgLength + 2){
                                handleMessage(readBuffer);
                                readBuffer.clear();
                            }
                        }
                        statusLED.write(false);
                    }else if(!mCommandQueue.isEmpty() && lastCmdSent > SEND_WAIT){
                        statusLED.write(true);
                        // Pop out the command from the Array
                        IBusCommand cmd = mCommandQueue.get(0);
                        // Send the message out
                        byte[] cmdBytes = genOutboundMessage(cmd);
                        // Write the message out to the bus byte by byte
                        for(byte cmdByte: cmdBytes){
                            busOut.write(cmdByte);
                        }
                        Log.d(TAG, 
                            String.format(
                                "Sending Command %s: %s",
                                cmd.commandType.toString(),
                                bytesToString(cmdBytes)
                            )
                        );
                        mCommandQueue.remove(0);
                        lastSend = getTime();
                        statusLED.write(false);
                    }
                }catch(IOException e){
                    Log.e(
                       TAG, String.format("Bus IOException: %s", e.getMessage())
                    );
                }
                Thread.sleep(2);
            }
            
            /**
             * Converts an ArrayList of bytes into a String
             * @param msgBuffer
             * @return String octal representation of bytes
             */
            private String bytesToString(ArrayList<Byte> msgBuffer){
                String data = "";
                for(byte msgByte: msgBuffer){
                    data += String.format("%02X ", msgByte);
                }
                return data;
            }
            
            /**
             * Converts a byte array into a String
             * @param msgBuffer
             * @return String octal representation of bytes
             */
            private String bytesToString(byte[] msgBuffer){
                String data = "";
                for(byte msgByte: msgBuffer){
                    data += String.format("%02X ", msgByte);
                }
                return data;
            }
                
            /**
             * Verify that the IBus Message is not corrupt by 
             * XORing all bytes if correct. The return should be 0x00
             * 
             * @param  msgBuffer  The buffer containing bytes
             * @return true if the message isn't corrupt, otherwise false
             */
            private boolean checksumMessage(ArrayList<Byte> msgBuffer){
                byte cksum = 0x00;
                for(byte msg: msgBuffer){
                    cksum = (byte) (cksum ^ msg);
                }
                return (cksum == 0x00) ? true : false;
            }
            
            /**
             * Called when IOIO disconnects
             */
            @Override
            public void disconnected(){
                Log.d(TAG, "IOIO Disconnect");
                IBusConn.close();
                mIsIOIOConnected = false;
            }
            
            /**
             * Generates bytes for the given message
             * 
             * @param command The IBusCommand object to create bytes for
             * @return Array of bytes containing the message for the bus
             */
            private byte[] genOutboundMessage(IBusCommand command){
                byte[] cmdBytes = new byte[]{};
                // Get the command type enum
                IBusCommand.Commands cmdType = command.commandType;
                // Get the instance of the class which implements this method
                IBusSystem clsInstance = IBusSysMap.get(
                    cmdType.getSystem().toByte()
                );
                // Get the command arguments
                Object cmdArgs = command.commandArgs;
                try{
                    Class<? extends IBusSystem> cls = clsInstance.getClass();
                    String methodName = cmdType.getMethodName();
                    // If cmdArgs is null, this send the method statically.
                    if(cmdArgs == null){
                        Method cmdMethod = cls.getMethod(methodName);
                        cmdBytes = (byte[]) cmdMethod.invoke(clsInstance);
                    }else{
                        Method cmdMethod = cls.getMethod(methodName, Object[].class);
                        cmdBytes = (byte[]) cmdMethod.invoke(clsInstance, cmdArgs);
                    }
                }catch(IllegalAccessException | IllegalArgumentException | 
                        InvocationTargetException | NoSuchMethodException e){
                    Log.e(
                        TAG,
                        String.format(
                            "Error invoking method in outbound queue: %s - %s",
                            e.toString(),
                            e.getMessage()
                        )
                    );
                }
                return cmdBytes;
            }
            
            /**
             * Get the current time in milliseconds
             * @return long Time in milliseconds
             */
            private long getTime(){
                return Calendar.getInstance().getTimeInMillis();
            }
                
            /**
             * Send the message to the correct handler 
             * @param msg
             */
            private void handleMessage(ArrayList<Byte> msg){
                byte src = msg.get(MSG_SRC_IDX);
                byte dest = msg.get(MSG_DEST_IDX);
                if(checksumMessage(msg)){
                    Log.d(TAG, String.format(
                        "Received Message (%s -> %s): %s",
                        mDeviceLookup.get(src), mDeviceLookup.get(dest),
                        bytesToString(msg)
                    ));
                    IBusSystem system = IBusSysMap.get(dest);
                    if(system != null){
                        system.mapReceived(msg);
                    }
                }else{
                    logBufferError("Message failed checksum test", msg);
                }
            }
            
            /**
             * Log buffer errors
             * @param err The error message
             * @param msg The buffer at the time of the error
             */
            private void logBufferError(String err, ArrayList<Byte> msg){
                Log.e(TAG, String.format("%s: %s", err, bytesToString(msg)));
            }
            
            
        };
    }
    
    public boolean getLinkState(){
        return mIsIOIOConnected;
    }
    
    public void registerCallback(IBusSystem.Callbacks cb, Handler handler){
        if(!IBusSysMap.isEmpty()){
            for (IBusSystem sys: IBusSysMap.values()){
                sys.registerCallback(cb, handler);
            }
        }
    }
    
    /**
     * Add an action into the queue of message waiting to be sent
     * @param cmd IBusCommand instance to be performed
     */
    public void sendCommand(IBusCommand cmd){
        mCommandQueue.add(cmd);
    }
    
    public void unregisterCallback(IBusSystem.Callbacks cb){
        if(!IBusSysMap.isEmpty()){
            for (IBusSystem sys: IBusSysMap.values()){
                sys.unregisterCallback(cb);
            }
        }
    }
    
    @Override
    public IBinder onBind(Intent intent){
        mClientsConnected++;
        return mBinder;
    }
    
    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStartCommand()");
        if(mDeviceLookup.isEmpty()){
            for(IBusSystem.Devices d : IBusSystem.Devices.values()){
                mDeviceLookup.put(d.toByte(), d.name());
            }
        }
        // Initiate values for IBus System handlers
        if(IBusSysMap.isEmpty()){
            Log.d(TAG, "Filling IBus System Map");
            IBusSysMap.put(
                IBusSystem.Devices.BoardMonitor.toByte(),
                new BoardMonitorSystem()
            );
            IBusSysMap.put(
                IBusSystem.Devices.Broadcast.toByte(),
                new BroadcastSystem()
            );
            IBusSysMap.put(
                IBusSystem.Devices.FrontDisplay.toByte(),
                new FrontDisplay()
            );
            IBusSysMap.put(
                IBusSystem.Devices.GFXNavigationDriver.toByte(),
                new GFXNavigationSystem()
            );
            IBusSysMap.put(
                IBusSystem.Devices.GlobalBroadcast.toByte(),
                new GlobalBroadcastSystem()
            );
            IBusSysMap.put(
                IBusSystem.Devices.InstrumentClusterElectronics.toByte(),
                new IKESystem()
            );
            IBusSysMap.put(
                IBusSystem.Devices.Radio.toByte(),
                new RadioSystem()
            );
            IBusSysMap.put(
                IBusSystem.Devices.MultiFunctionSteeringWheel.toByte(),
                new SteeringWheelSystem()
            );
            IBusSysMap.put(
                IBusSystem.Devices.Telephone.toByte(),
                new TelephoneSystem()
            );
        }
        return START_STICKY;
    }
    
    @Override
    public boolean onUnbind(Intent intent){
        super.onUnbind(intent);
        Log.d(TAG, "onUnbind()");
        mClientsConnected--;
        if(mClientsConnected == 0){
            stopSelf();
        }
        return false;
    }

}