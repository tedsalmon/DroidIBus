package net.littlebigisland.droidibus;
// I should deprecate this since it's not even part of my envisioned structure
public class IBusEnums {
	public enum Actions {
		VOL_UP,
		VOL_DOWN,
		MODE_CHG, 
		NEXT_BTN,
		PREV_BTN,
		MPG_RST,
		AVGSPD_RST,
		MPG_GET,
		TEMP_GET,
	}
	
	public enum Callbacks{
		SPD_UPDATE,
		MPG1_UPDATE,
		MPG2_UPDATE,
		AVGSPD_UPDATE,
		COOLANTTMP_UPDATE,
		
	}
}