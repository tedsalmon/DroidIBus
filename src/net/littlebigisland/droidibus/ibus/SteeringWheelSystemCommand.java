package net.littlebigisland.droidibus.ibus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SteeringWheelSystemCommand extends IBusSystemCommand{
	
	private Map<Byte, IBusSystemCommand> IBusMFSWMap = new HashMap<Byte, IBusSystemCommand>();
	
	class Radio extends IBusSystemCommand{

		public void mapReceived(ArrayList<Byte> msg) {
			currentMessage = msg;
			if(currentMessage.get(4) == 0x3B){
				switch(currentMessage.get(4)){
					case 0x21: //Fwds Btn
						if(mCallbackReceiver != null)
							mCallbackReceiver.onTrackFwd();
						break;
					case 0x28: //Prev Btn
						if(mCallbackReceiver != null)
							mCallbackReceiver.onTrackPrev();
						break;
				}
			}
		}
		
	}
	
	public void mapReceived(ArrayList<Byte> msg) {
		if(IBusMFSWMap.isEmpty()){
			IBusMFSWMap.put(DeviceAddress.Radio.toByte(), new Radio());
			// Register the callback listener here ;)
			for (Object key : IBusMFSWMap.keySet())
				IBusMFSWMap.get(key).registerCallbacks(mCallbackReceiver);
		}
		try{
			IBusMFSWMap.get((byte) msg.get(2)).mapReceived(msg);
		}catch(NullPointerException npe){
			// Things not in the map throw a NullPointerException
		}
	}

}
