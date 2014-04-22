/**
 * Part of the DroidIBus Project
 *
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 *
 */
package net.littlebigisland.droidibus;

/**
 * Interface to build call backs off of
 */
public interface IBusMessageReceiver{

	public void onUpdateStation(final String text);

	public void onUpdateRange(final String range);
		
	public void onUpdateOutdoorTemp(final String temp);

	public void onUpdateFuel1(final String mpg);

	public void onUpdateFuel2(final String mpg);

	public void onUpdateAvgSpeed(final String speed);
	
	public void onUpdateTime(final String time);
	
	public void onUpdateDate(final String date);
	
	public void onUpdateSpeed(final String speed);

	public void onUpdateRPM(final String rpm);
	
	public void onUpdateCoolantTemp(final String temp);
	
	public void onUpdateIgnitionSate(final int state);
	
}