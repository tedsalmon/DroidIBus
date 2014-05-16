package net.littlebigisland.droidibus.ibus;

import java.util.ArrayList;
import java.util.List;

public class NavigationSystemCommand extends IBusSystemCommand {
	
	class Telephone extends IBusSystemCommand{
		public void mapReceived(ArrayList<Byte> msg) {
			// TODO Auto-generated method stub
			
		}
		
	}
	//class GPSSystemCommand
	/*
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
		break;*/

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
	
	public void mapReceived(ArrayList<Byte> msg) {
		// TODO Auto-generated method stub
		
	}
}
