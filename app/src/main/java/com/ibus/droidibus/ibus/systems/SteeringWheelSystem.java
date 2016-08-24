package com.ibus.droidibus.ibus.systems;

import com.ibus.droidibus.ibus.IBusSystem;

public class SteeringWheelSystem extends IBusSystem{
    
    final private byte mRadioAddress = Devices.Radio.toByte();
    final private byte mSteeringWheelAddress = Devices.MultiFunctionSteeringWheel.toByte();
    
    
    public byte[] sendVolumeUp(){
        return new byte[]{
            mSteeringWheelAddress, 0x04, mRadioAddress, 0x32, 0x11, 0x1F
        };
    }
    
    public byte[] sendVolumeDown(){
        return new byte[]{
            mSteeringWheelAddress, 0x04, mRadioAddress, 0x32, 0x10, 0x1E
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
