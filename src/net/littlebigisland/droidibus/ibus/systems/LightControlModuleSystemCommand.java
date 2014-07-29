package net.littlebigisland.droidibus.ibus.systems;

import java.util.ArrayList;

import net.littlebigisland.droidibus.ibus.DeviceAddressEnum;
import net.littlebigisland.droidibus.ibus.IBusSystemCommand;

public class LightControlModuleSystemCommand extends IBusSystemCommand{
	
	class GlobalBroadcast extends IBusSystemCommand{

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
	 * Cstruct - Register destination systems
	 */
	public LightControlModuleSystemCommand(){
		IBusDestinationSystems.put(DeviceAddressEnum.GlobalBroadcastAddress.toByte(), new GlobalBroadcast());
	}
}