package net.pushl.scratch;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        Scratch scratch = new Scratch();
        scratch.add_eventlistener(new ScratchEventListener() {
            @Override
            public void start_scratch(Scratch s) {
                super.start_scratch(s);
                System.err.println("Scratch connected");
            }
            @Override
            public void end_scratch(Scratch s) {
                super.end_scratch(s);
                System.err.println("Scratch disconnected");
            }
            @Override
            public void receive_broadcast(Scratch s,String message) {
                super.receive_broadcast(s,message);
                System.err.println("receive: "+message);
                if(message.equals("poyo")){
                    try {
                        s.send_broadcast("hoge");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void receive_sensor_update(Scratch s,String name, String val) {
                // if name is double quoted,it's string.
                super.receive_sensor_update(s,name, val);
                System.err.println(name + "->" + val);
                System.err.println(s.var(name));
            }
        });
        try {
            scratch.open_with_recv_thread("localhost");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
