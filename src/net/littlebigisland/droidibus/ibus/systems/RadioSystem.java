package net.littlebigisland.droidibus.ibus.systems;

import java.util.ArrayList;

import net.littlebigisland.droidibus.ibus.IBusSystem;

public class RadioSystem extends IBusSystem{
    
    /** 
     * Messages from the MFL to the Radio
     */
    class SteeringWheelSystem extends IBusSystem{
        
        public void mapReceived(ArrayList<Byte> msg) {
            currentMessage = msg;
            if(currentMessage.get(3) == 0x3B){
                switch(currentMessage.get(4)){
                    case 0x21: //Fwds Btn
                        triggerCallback("onTrackFwd");
                        break;
                    case 0x28: //Prev Btn
                        triggerCallback("onTrackPrev");
                        break;
                }
            }
        }
        
    }
    
    public RadioSystem(){
        IBusDestinationSystems.put(
            Devices.MultiFunctionSteeringWheel.toByte(),
            new SteeringWheelSystem()
        );
    }
    
}
