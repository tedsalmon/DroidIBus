package net.littlebigisland.droidibus.ibus.systems;

import java.util.ArrayList;

import net.littlebigisland.droidibus.ibus.DeviceAddressEnum;
import net.littlebigisland.droidibus.ibus.IBusSystemCommand;

public class SteeringWheelSystemCommand extends IBusSystemCommand{
	final private byte mRadioAddress = DeviceAddressEnum.Radio.toByte();
	final private byte mSteeringWheelAddress = DeviceAddressEnum.MultiFunctionSteeringWheel.toByte();
	final private byte mTelephoneAddress = DeviceAddressEnum.Telephone.toByte();
	
	class Radio extends IBusSystemCommand{

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
	
	class Telephone extends IBusSystemCommand{

		public void mapReceived(ArrayList<Byte> msg) {
			currentMessage = msg;
			if(currentMessage.get(3) == 0x3B){
				if(currentMessage.get(4) == (byte) 0xA0) // Voice Btn
					triggerCallback("onVoiceBtnPress");
			}
		}
		
	}
	
	/**
	 * Cstruct - Register destination systems
	 */
	public SteeringWheelSystemCommand(){
		IBusDestinationSystems.put(mRadioAddress, new Radio());
		IBusDestinationSystems.put(mTelephoneAddress, new Telephone());
	}
	
	public byte[] sendVolumeUp(){
		return new byte[]{
			mSteeringWheelAddress, 0x04, mRadioAddress, 0x32, 0x31, 0x3F
		};
	}
	
	public byte[] sendVolumeDown(){
		return new byte[]{
			mSteeringWheelAddress, 0x04, mRadioAddress, 0x32, 0x30, 0x3E
		};
	}
	
	public byte[] sendTuneFwdPress(){
		return new byte[]{
			mSteeringWheelAddress, 0x04, mRadioAddress, 0x3B, 0x01, 0x06
		};
	}
	
	public byte[] sendTuneFwdRelease(){
		return new byte[]{
			mSteeringWheelAddress, 0x04, mRadioAddress, 0x3B, 0x21, 0x26
		};
	}
	
	public byte[] sendTunePrevPress(){
		return new byte[]{
			mSteeringWheelAddress, 0x04, mRadioAddress, 0x3B, 0x08, 0x0F
		};
	}
	
	public byte[] sendTunePrevRelease(){
		return new byte[]{
			mSteeringWheelAddress, 0x04, mRadioAddress, 0x3B, 0x28, 0x2F
		};
	}
}
