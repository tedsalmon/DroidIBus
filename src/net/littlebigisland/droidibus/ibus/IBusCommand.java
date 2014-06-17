package net.littlebigisland.droidibus.ibus;

public class IBusCommand {
	IBusCommandsEnum commandType = null;
	Object commandArgs = null;
	
	public IBusCommand(IBusCommandsEnum cmd, Object... args){
		commandType = cmd;
		if(args.length > 0)
			commandArgs = args;
	}
}