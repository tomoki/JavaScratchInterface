package net.pushl.scratch;

public abstract class ScratchEventListener {

    public ScratchEventListener(){
    }
    public void start_scratch(Scratch s){
    }
    public void end_scratch(Scratch s){
    }
    public void receive_broadcast(Scratch s,String message){
    }
    public void receive_variable_update(Scratch s,String name,String val){
    }
}
