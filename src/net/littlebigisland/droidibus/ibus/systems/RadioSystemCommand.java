package net.littlebigisland.droidibus.ibus.systems;

import java.util.ArrayList;

import net.littlebigisland.droidibus.ibus.DeviceAddressEnum;
import net.littlebigisland.droidibus.ibus.IBusSystemCommand;

public class RadioSystemCommand extends IBusSystemCommand{
	
	/** 
	 * Messages from the MFL to the Radio
	 */
	class SteeringWheelSystem extends IBusSystemCommand{
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
	
	public RadioSystemCommand(){
		IBusDestinationSystems.put(DeviceAddressEnum.MultiFunctionSteeringWheel.toByte(), new SteeringWheelSystem());
	}
	
}
