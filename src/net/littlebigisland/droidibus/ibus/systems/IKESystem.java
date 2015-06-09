package net.littlebigisland.droidibus.ibus.systems;
import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;

import net.littlebigisland.droidibus.ibus.IBusSystem;

public class IKESystem extends IBusSystem{

    /**
     * Messages from the Telephone to the IKE
     */
    class TelephoneSystem extends IBusSystem{
        
        @SuppressLint("UseSparseArrays")
        private HashMap<Byte, byte[]> byteMapping = new HashMap<Byte, byte[]>();
        
        /**
         * convertSpecialBytes() - Take the IBus specific bytes that the
         * LCDs in the car can display and convert them to their UTF 
         * @param data The data to look over
         * @param start The first byte to inspect
         * @param end   The last byte to inspect
         * @return         The ArrayList of bytes converted
         */
        private ArrayList<Byte> convertSpecialBytes(ArrayList<Byte> data, int start, int end){
            ArrayList<Byte> returnData = new ArrayList<Byte>();
            int position = start;
            while(position <= end){
                byte curByte = data.get(position);
                byte[] map = byteMapping.get(curByte);
                if(map != null){
                    for(int i = 0; i < map.length; i++){
                        returnData.add(map[i]);
                    }
                }else{
                    returnData.add(curByte);
                }
                position++;
            }
            return returnData;
        }
        
        public void mapReceived(ArrayList<Byte> msg) {
            currentMessage = msg;
            if(currentMessage.get(3) == 0x23){
                // IKE Display C8 12 80 23 42 <DATA> <CRC>
                if(currentMessage.get(4) == 0x42){
                    ArrayList<Byte> msgData = convertSpecialBytes(currentMessage, 6,  currentMessage.size()-2);
                    triggerCallback("onUpdateIKEDisplay", decodeMessage(msgData, 0, msgData.size()-1));
                }
            }
        }
        
        TelephoneSystem(){
            // Add the bytes we need to convert from the IKE Display to the mapping HashMap
            byteMapping.put((byte) 0xAD, new byte[] {(byte) 0xE2, (byte)0x96, (byte)0xB2}); // Up Arrow
            byteMapping.put((byte) 0xAE, new byte[] {(byte)0xE2, (byte)0x96, (byte)0xBC}); // Down Arrow
            byteMapping.put((byte) 0xB2, new byte[] {(byte)0x37}); // 7 bars -> #6
            byteMapping.put((byte) 0xB3, new byte[] {(byte)0x36}); // 6 bars -> #5
            byteMapping.put((byte) 0xB4, new byte[] {(byte)0x35}); // 5 bars -> #4
            byteMapping.put((byte) 0xB5, new byte[] {(byte)0x34}); // 4 bars -> #3
            byteMapping.put((byte) 0xB6, new byte[] {(byte)0x33}); // 3 bars -> #2
            byteMapping.put((byte) 0xB7, new byte[] {(byte)0x32}); // 2 bars -> #1
            byteMapping.put((byte) 0xB8, new byte[] {(byte)0x31}); // 1 Bars -> #0
        }
        
    }

    
    public IKESystem(){
        IBusDestinationSystems.put(
            Devices.Telephone.toByte(), new TelephoneSystem()
        );
    }
    
}