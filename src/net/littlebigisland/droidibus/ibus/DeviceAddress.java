package net.littlebigisland.droidibus.ibus;

/**
 * The address off all the systems linked via IBus
 */
enum DeviceAddress {
	// System constants
	BodyModule((byte) 0x00),
	SunroofControl((byte) 0x08),
	CDChanger((byte) 0x18),
	RadioControlledClock((byte) 0x28),
	CheckControlModule((byte) 0x30),
	GraphicsNavigationDriver((byte) 0x3B),
	Diagnostic((byte) 0x3F),
	RemoteControlCentralLocking((byte) 0x40),
	GraphicsDriverRearScreen((byte) 0x43),
	Immobiliser((byte) 0x44),
	CentralInformationDisplay((byte) 0x46),
	MultiFunctionSteeringWheel((byte) 0x50),
	MirrorMemory((byte) 0x51),
	IntegratedHeatingAndAirConditioning((byte) 0x5B),
	ParkDistanceControl((byte) 0x60),
	Radio((byte) 0x68),
	DigitalSignalProcessingAudioAmplifier((byte) 0x6A),
	SeatMemory((byte) 0x72),
	SiriusRadio((byte) 0x73),
	CDChangerDINsize((byte) 0x76),
	NavigationEurope((byte) 0x7F),
	InstrumentClusterElectronics((byte) 0x80),
	MirrorMemorySecond((byte) 0x9B),
	MirrorMemoryThird((byte) 0x9C),
	RearMultiInfoDisplay((byte) 0xA0),
	AirBagModule((byte) 0xA4),
	SpeedRecognitionSystem((byte) 0xB0),
	NavigationJapan((byte) 0xBB),
	GlobalBroadcastAddress((byte) 0xBF),
	MultiInfoDisplay((byte) 0xC0),
	Telephone((byte) 0xC8),
	Assist((byte) 0xCA),
	LightControlModule((byte) 0xD0),
	SeatMemorySecond((byte) 0xDA),
	IntegratedRadioInformationSystem((byte) 0xE0),
	FrontDisplay((byte) 0xE7),
	RainLightSensor((byte) 0xE8),
	Television((byte) 0xED),
	OnBoardMonitor((byte) 0xF0),
	Broadcast((byte) 0xFF),
	Unset((byte) 0x100),
	Unknown((byte) 0x101);
	
	private final byte value;
	
	DeviceAddress(byte value) {
	    this.value = value;
	}

	public byte toByte(){
		return value;
	}
	
}
