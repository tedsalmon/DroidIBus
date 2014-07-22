/**
 * Part of the DroidIBus Project
 *
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 *
 */
package net.littlebigisland.droidibus.ibus;

/**
 * Interface to build call backs off of
 */
public abstract interface IBusMessageReceiver{

	// Radio System
	public void onUpdateRadioStation(final String text);
	
	public void onUpdateRadioBrodcasts(final String broadcastType);
	
	public void onUpdateRadioStereoIndicator(final String stereoIndicator);
	
	public void onUpdateRadioRDSIndicator(final String rdsIndicator);
	
	public void onUpdateRadioProgramIndicator(final String currentProgram);
	
	public void onUpdateRadioStatus(final int status);
	
	public void onRadioCDStatusRequest();
	
	// IKE System
	public void onUpdateRange(final String range);
		
	public void onUpdateOutdoorTemp(final String temp);

	public void onUpdateFuel1(final String mpg);

	public void onUpdateFuel2(final String mpg);

	public void onUpdateAvgSpeed(final String speed);
	
	public void onUpdateTime(final String time);
	
	public void onUpdateDate(final String date);
	
	public void onUpdateSpeed(final int speed);

	public void onUpdateRPM(final int rpm);
	
	public void onUpdateCoolantTemp(final int temp);
	
	public void onUpdateIgnitionSate(final int state);
	
	// Navigation System
	public void onUpdateStreetLocation(final String streetName);
	
	public void onUpdateGPSAltitude(final int altitude);
	
	public void onUpdateGPSCoordinates(final String gpsCoordinates);
	
	public void onUpdateGPSTime(final String time);
	
	public void onUpdateLocale(final String cityName);
	
	// Steering Wheel System	
	public void onTrackFwd();
	
	public void onTrackPrev();
	
	public void onVoiceBtnPress();
	
	
	// Light Control System
	public void onLightStatus(final int lightStatus);
}