package net.littlebigisland.droidibus.ibus.systems;

import java.util.ArrayList;

import net.littlebigisland.droidibus.ibus.DeviceAddressEnum;
import net.littlebigisland.droidibus.ibus.IBusSystemCommand;

public class GlobalBroadcastSystemCommand extends IBusSystemCommand{
    
    /**
     * Messages from the IKE to the GlobalBroadcast
     */
    class IKESystem extends IBusSystemCommand{
        
        public void mapReceived(ArrayList<Byte> msg){
            switch(msg.get(3)){
                case 0x11: // Ignition State
                    int state = (msg.get(4) < 2) ? msg.get(4) : (0x02 & msg.get(4));
                    triggerCallback("onUpdateIgnitionSate", state);
                    break;
                case 0x15: // Unit Set
                    triggerCallback(
                        "onUpdateUnits", 
                        String.format(
                            "%8s;%8s", 
                            Integer.toBinaryString(msg.get(5) & 0xFF),
                            Integer.toBinaryString(msg.get(6) & 0xFF)
                        ).replace(' ', '0')
                    );
                    break;
                case 0x18: // Speed and RPM
                    triggerCallback("onUpdateSpeed", (int)msg.get(4));
                    triggerCallback("onUpdateRPM", (int)msg.get(5) * 100);
                    break;
                case 0x19: // Coolant Temperature
                    triggerCallback("onUpdateCoolantTemp", (int)msg.get(5));
                    break;
            }
        }
        
    }
    
    /**
     * Messages from the LCM to the GlobalBroadcast
     */
    class LightControlModuleSystem extends IBusSystemCommand{

        public void mapReceived(ArrayList<Byte> msg) {
            currentMessage = msg;
            // 0x5C is the light dimmer status. It appears FF = lights off and FE = lights on
            if(currentMessage.get(3) == 0x5C){
                int lightStatus = (currentMessage.get(4) == (byte) 0xFF) ? 0 : 1;
                triggerCallback("onLightStatus", lightStatus);
            }
        }
        
    }
    
    public GlobalBroadcastSystemCommand(){
        IBusDestinationSystems.put(DeviceAddressEnum.InstrumentClusterElectronics.toByte(), new IKESystem());
        IBusDestinationSystems.put(DeviceAddressEnum.LightControlModule.toByte(), new LightControlModuleSystem());
    }
}