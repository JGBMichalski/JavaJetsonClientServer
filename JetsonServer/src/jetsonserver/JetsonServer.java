package jetsonserver;

import java.io.*;
import java.net.Socket;

public class JetsonServer implements Runnable {
     private Socket pingSocket = null;
     private PrintWriter out = null;
     private BufferedReader in = null;
     private boolean sentMode = false;
     private GUI gui;
     private int delay;
     
     public JetsonServer(GUI x){
         gui = x;
         delay = 6000;
     }

    public void send(String x) throws IOException {
        out.println(x + ";");
    }
    
    public boolean connect(){
        try {
            pingSocket = new Socket("10.42.0.121", 23);
            out = new PrintWriter(pingSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(pingSocket.getInputStream()));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    public boolean disconnect(){
        try {
            out.close();
            in.close();
            pingSocket.close();
            return true;
        } catch (IOException e){
            return false;
        }
    }
    
    //Sentry Mode
    public void run(){
        while (true){
            if (sentMode){
                try{
                	//gui.display("Sentry Mode enabled.");
                    send("x+500");
                    Thread.sleep(delay);
                    send("x-999");
                    Thread.sleep(delay*2);
                    send("x+500");
                    Thread.sleep(delay);
                } catch (Exception e){}
            } else {
                try{
                	//gui.display("Sentry Mode disabled.");
                    Thread.sleep(1000);
                } catch (Exception e){}
            }
        }
    }
    
    public void setMode(boolean x){
        sentMode = x;
    }
    
    public void setTime(int x) {
    	delay = x;
    }
}
