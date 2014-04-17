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
public interface IBusMessageListener{

	public void onUpdateStation(final String text);

	public void onUpdateSpeed(final String speed);

	public void onUpdateRPM(final String rpm);

	public void onUpdateRange(final String range);
		
	public void onUpdateOutTemp(final String temp);

	public void onUpdateCoolantTemp(final String temp);

	public void onUpdateFuel1(final String mpg);

	public void onUpdateFuel2(final String mpg);

	public void onUpdateAvgSpeed(final String speed);
	
}