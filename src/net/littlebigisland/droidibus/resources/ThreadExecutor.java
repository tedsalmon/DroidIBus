package net.littlebigisland.droidibus.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class ThreadExecutor implements Executor{
    
    // Thread List
    protected List<Thread> mThreadList = new ArrayList<Thread>();
    
    @Override
    public void execute(Runnable command) {
        Thread childThread = new Thread(command);
        mThreadList.add(childThread);
        childThread.start();
    }
    
    public void terminateTasks(){
        for(Thread childThread: mThreadList){
            childThread.interrupt();
        }
    }
    
}