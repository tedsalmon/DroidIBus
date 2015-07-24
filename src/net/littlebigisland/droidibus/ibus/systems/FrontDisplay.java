package net.littlebigisland.droidibus.ibus.systems;

public class FrontDisplay extends GlobalBroadcastSystem{
    
    /**
     * FrontDisplay is a system used on older E3x models
     * 
     * FrontDisplay extends GlobalBroadcast because the IKE functionality
     * is exactly the same. We must however clear the Destination systems
     * and add the ones we need as Java parent construct calls are implicit.
     */
    public FrontDisplay(){
        super();
        IBusDestinationSystems.clear();
        
        IBusDestinationSystems.put(
            Devices.InstrumentClusterElectronics.toByte(), new IKESystem()
        );
    }

}
