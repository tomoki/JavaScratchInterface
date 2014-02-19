package net.pushl.scratch;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Scratch {
    public static final int SCRATCH_PORT = 42001;
    public static final String SENSOR_UPDATE = "sensor-update";
    public static final String BROADCAST = "broadcast";
    private Socket socket = new Socket();
    private HashMap<String,String> variables = new HashMap<>();
    private HashMap<String,String> sensor_values = new HashMap<>();
    private ArrayList<ScratchEventListener> listeners = new ArrayList<>();

    public Scratch(){
    }
    public void add_eventlistener(ScratchEventListener listener){
        listeners.add(listener);
    }
    public void open(final String ip_addr) throws IOException{
        if(socket.isConnected()){
            System.err.println("already opened.");
            return;
        }
        socket.connect(new InetSocketAddress(ip_addr,SCRATCH_PORT));
    }
    public void open_with_recv_thread(final String ip_addr) throws IOException{
        open(ip_addr);
        new Thread(new ScratchThread()).start();
    }
    public void close() throws IOException{
        if(socket.isClosed()){
            System.err.println("already closed");
            return;
        }
        socket.close();
    }
    public boolean is_connected(){
        return socket.isConnected();
    }
    public void send_broadcast(final String s) throws IOException{
        send_message(BROADCAST + " \"" + s + "\"");
    }
    public boolean is_known_var(final String s){
        return variables.containsKey(s);
    }

    public String var(final String s){
        return variables.get(s);
    }

    public void set_sensor_value(final String name,
                                 final String val) throws IOException{
        sensor_values.put(name, val);
        send_message(SENSOR_UPDATE + " \"" + name + "\" " + val);
    }

    public String get_sensor_value(final String name){
        return sensor_values.get(name);
    }

    // Helper functions
    private void send_message(final String s) throws IOException{
        // System.err.println(s);
        // System.err.println(to_scratch_message(s));
        send_message(to_scratch_message(s));
    }

    private void send_message(final byte[] bytes) throws IOException{
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        os.write(bytes);
    }

    private byte[] to_scratch_message(final String s){
        int n = s.length();
        ArrayList<Byte> ret = new ArrayList<Byte>();
        ret.add((byte)((n >> 24) & 0xFF));
        ret.add((byte)((n >> 16) & 0xFF));
        ret.add((byte)((n >> 8) & 0xFF));
        ret.add((byte)(n & 0xFF));
        for(byte b : s.getBytes()){
            ret.add(b);
        }
        // TODO: I think,there is more clever way.
        byte[] r = new byte[ret.size()];
        for(int i=0;i<ret.size();i++){
            r[i] = ret.get(i);
        }
        return r;
    }

    private void receive_data() throws IOException{
        DataInputStream in = new DataInputStream(socket.getInputStream());
        int n;byte[] recv = new byte[1024];
        n = in.read(recv);
        // Trush first 4.
        String message = new String(recv,4,n-4,"UTF-8");
        proceed_message(message);
    }

    private void proceed_message(final String message){
        //System.err.println(message);
        if(message.startsWith(BROADCAST)){
            proceed_broadcast(message);;
        }else if(message.startsWith(SENSOR_UPDATE)){
            proceed_variable_update(message);
        }else{
            System.err.println("unknown message: " + message);
        }
    }

    private void proceed_variable_update(String message){
        // remove sensor_update and add space to last.(it's gard.)
        message = message.replaceFirst(SENSOR_UPDATE+" ","") + " ";
        ArrayList<String> splited = new ArrayList<>();
        String cur = "";
        boolean isFirstCharacter = true;
        boolean isFirstisQuotation = true;

        for(int i=0;i<message.length()-1;i++){
            if(isFirstCharacter){
                cur = "";
                if(message.charAt(i) == '\"'){
                    isFirstisQuotation = true;
                }else if(message.charAt(i) == ' '){
                    // tail space.ignore it.
                }else{
                    isFirstisQuotation = false;
                    cur += message.charAt(i);
                }
                isFirstCharacter = false;
            }else{
                if(!isFirstisQuotation){
                    if(message.charAt(i) == ' '){
                        splited.add(cur);
                        isFirstCharacter = true;
                    }else{
                        cur += message.charAt(i);
                    }
                }else{
                    if(message.charAt(i) == '\"'){
                        if(message.charAt(i+1) == '\"'){
                            // it is escaped.
                            cur += "\"";i++;
                        }else if(message.charAt(i+1) == ' '){
                            // last quotation.
                            splited.add(cur);i++;
                            isFirstCharacter = true;
                        }else{
                            // there is no such case?
                        }
                    }else{
                        cur += message.charAt(i);
                    }
                }
            }
        }
        for(int i=0;i<splited.size();i+=2){
            String name = splited.get(i);
            // remove double quote.
            // name = name.substring(1,name.length()-1);
            String value = splited.get(i+1);
            variables.put(name, value);
            for(ScratchEventListener listener : listeners){
                listener.receive_variable_update(this,name,value);
            }
        }
    }

    private void proceed_broadcast(String message){
        // remove broadcast and remove double quotatin.
        message = message.replaceFirst(BROADCAST+" ","");
        message = message.substring(1, message.length()-1);
        for(ScratchEventListener listener : listeners){
            listener.receive_broadcast(this,message);
        }
    }
    private class ScratchThread implements Runnable{
        @Override
        public void run() {
            for(ScratchEventListener listener : listeners){
                listener.start_scratch(Scratch.this);
            }
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    receive_data();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            for(ScratchEventListener listener : listeners){
                listener.end_scratch(Scratch.this);
            }
        }
    }
}
