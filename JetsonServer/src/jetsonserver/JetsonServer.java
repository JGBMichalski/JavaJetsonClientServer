package jetsonserver;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class JetsonServer implements Runnable {
     private Socket pingSocket = null;
     private PrintWriter out = null;
     private BufferedReader in = null;
     private boolean sentMode = false;
     private GUI gui;
     
     public JetsonServer(GUI x){
         gui = x;
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
                	gui.display("Sentry Mode enabled.");
                    send("x+999");
                    Thread.sleep(15000);
                    send("x-999");
                    Thread.sleep(15000);
                } catch (Exception e){}
            } else {
                try{
                	gui.display("Sentry Mode disabled.");
                    Thread.sleep(15000);
                } catch (Exception e){}
            }
        }
    }
    
    public void setMode(boolean x){
        sentMode = x;
    }
}
