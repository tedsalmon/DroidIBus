package net.littlebigisland.droidibus.ibus.systems;

import java.util.ArrayList;

import net.littlebigisland.droidibus.ibus.IBusSystem;

public class GlobalBroadcastSystem extends IBusSystem{
    
    private static final byte IKE_SYSTEM = Devices.InstrumentClusterElectronics.toByte();
    private static final byte GLOBAL_BROADCAST = Devices.GlobalBroadcast.toByte();
    
    /**
     * Messages from the IKE to the GlobalBroadcast
     */
    class IKESystem extends IBusSystem{
        private static final byte IGN_STATE = 0x11;
        private static final byte OBC_UNITSET = 0x15;
        private static final byte SPEED_RPM = 0x18;
        private static final byte MILEAGE = 0x17;
        private static final byte COOLANT_TEMP = 0x19;
        
        public void mapReceived(ArrayList<Byte> msg){
            switch(msg.get(3)){
                case IGN_STATE:
                    int state = (msg.get(4) < 2) ? msg.get(4) : (0x02 & msg.get(4));
                    triggerCallback("onUpdateIgnitionSate", state);
                    break;
                case OBC_UNITSET:
                    triggerCallback(
                        "onUpdateUnits", 
                        String.format(
                            "%8s;%8s", 
                            Integer.toBinaryString(msg.get(5) & 0xFF),
                            Integer.toBinaryString(msg.get(6) & 0xFF)
                        ).replace(' ', '0')
                    );
                    break;
                case SPEED_RPM:
                    triggerCallback("onUpdateSpeed", (int)msg.get(4));
                    triggerCallback("onUpdateRPM", (int)msg.get(5) * 100);
                    break;
                case MILEAGE:
                    // Bytes 5-7 contain the Mileage
                    // Bytes 8 and 9 hold the inspection interval
                    // Byte 10 is the SIA Type (0x40 == Inspection)
                    // Byte 11 is the the days to inspection.
                    int mileage = (
                        (currentMessage.get(7) * 65535) +
                        (currentMessage.get(6) * 256) +
                        currentMessage.get(5)
                    );
                    int serviceInterval = (
                        (currentMessage.get(8) + currentMessage.get(9)) * 50
                    );
                    int serviceIntervalType = currentMessage.get(10);
                    int daysToInspection = currentMessage.get(11);
                    triggerCallback("onUpdateMileage", mileage);
                    triggerCallback("onUpdateServiceInterval", serviceInterval);
                    triggerCallback(
                        "onUpdateDServiceIntervalType", serviceIntervalType
                    );
                    triggerCallback("onUpdateDaysToInspection", daysToInspection);
                    break;
                case COOLANT_TEMP:
                    triggerCallback("onUpdateCoolantTemp", (int)msg.get(5));
                    break;
            }
        }
        
    }
    
    /**
     * Messages from the LCM to the GlobalBroadcast
     */
    class LightControlModuleSystem extends IBusSystem{

        public void mapReceived(ArrayList<Byte> msg) {
            currentMessage = msg;
            // 0x5C is the light dimmer status. It appears FF = lights off and FE = lights on
            if(currentMessage.get(3) == 0x5C){
                int lightStatus = (currentMessage.get(4) == (byte) 0xFF) ? 0 : 1;
                triggerCallback("onLightStatus", lightStatus);
            }
        }
        
    }
    
    /**
     * Request mileage from the IKE
     * IBUS Message: BF 03 80 16 2A
     * @return byte[] Message for the IBus
     */
    public byte[] getMileage(){
        return new byte[]{
            GLOBAL_BROADCAST, 0x03, IKE_SYSTEM, 0x16, 0x2A
        };
    }
    
    public GlobalBroadcastSystem(){
        IBusDestinationSystems.put(
            Devices.InstrumentClusterElectronics.toByte(), new IKESystem()
        );
        IBusDestinationSystems.put(
            Devices.LightControlModule.toByte(), new LightControlModuleSystem()
        );
    }
}