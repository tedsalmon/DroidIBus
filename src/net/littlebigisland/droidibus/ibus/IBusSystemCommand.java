package net.littlebigisland.droidibus.ibus;

import java.util.ArrayList;

/**
 * This class represents the base of which all IBus Systems will extend from
 * in order to parse sent/received messages.
 * 
 * Subsequent subclasses implement message handlers for destination systems
 * using this same class. I.e. Source system IKE Extends IBusSystemCommand
 * it's child class, say GlobalBroadcastSystem also implements IBusSystemCommand
 */
public abstract interface IBusSystemCommand {
	public IBusMessageReceiver mCallbackReceiver = null;
	
	void mapReceived(ArrayList<Byte> msg);
}
