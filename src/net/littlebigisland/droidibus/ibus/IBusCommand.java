package net.littlebigisland.droidibus.ibus;

/**
 * This class will be used to send commands to the IBusService. 
 * This allows us to hold optional arguments for methods we call.
 */
public class IBusCommand{
    IBusCommandsEnum commandType = null;
    Object commandArgs = null;
    
    public IBusCommand(IBusCommandsEnum cmd, Object... args){
        commandType = cmd;
        if(args.length > 0){
            commandArgs = args;
        }
    }
}