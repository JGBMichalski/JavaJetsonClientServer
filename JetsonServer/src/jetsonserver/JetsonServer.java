package jetsonserver;

import java.io.*;
import java.net.Socket;

public class JetsonServer {
     static Socket pingSocket = null;
     static PrintWriter out = null;
     static BufferedReader in = null;

    public static void send(String x) throws IOException {
        out.println(x + ";");
    }
    
    public static boolean connect(){
        try {
            pingSocket = new Socket("10.42.0.121", 23);
            out = new PrintWriter(pingSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(pingSocket.getInputStream()));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    public static boolean disconnect(){
        try {
            out.close();
            in.close();
            pingSocket.close();
            return true;
        } catch (IOException e){
            return false;
        }
    }
}
