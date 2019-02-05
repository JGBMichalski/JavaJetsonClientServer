package jetsonserver;

import java.io.*;
import java.net.*;

public class clientConnection{

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String aqon = "";
    private String aqoff = "";
    private String deton = "";
    private String detoff= "";

    static boolean isConnected = false;
    static String cmd = "";
    static gui gui;
    static clientConnection server;

    public static void main(String[] args) {
        server = new clientConnection();
        gui = new gui(server);
        gui.setVisible(true);
        server.start(8080);
        while (isConnected = true){
            cmd = server.listen();
        }
    }

    public Socket start(int port){
        try {
            gui.display("Setting up socket...");
            serverSocket = new ServerSocket(port);
            gui.display("Waiting for client...");
            clientSocket = serverSocket.accept();
            isConnected = true;

            gui.display("Connected to client");
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e){
            e.printStackTrace();
        } 
        return clientSocket;
    }

    public void stop(){
        try {
            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        gui.display("Server stopped.");
        isConnected = false;
    }

    public String listen(){
        String msg = new String();
        try {
            InputStreamReader inputStream = new InputStreamReader(clientSocket.getInputStream());
            in = new BufferedReader(inputStream);
            while ((msg = in.readLine()) != null){
                gui.display("Received: "+ msg);
                handleCmd(msg);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return cmd;
    }

    public void executeCMD(String CMD) {
        String s;
        Process p;
        try {
            p = Runtime.getRuntime().exec(CMD);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((s = br.readLine()) != null)
                gui.display(s);
            p.waitFor();
            System.out.println (p.exitValue());
            p.destroy();
        } catch (Exception e) {
            gui.display("Could not execute command: " + CMD);
        }
    }
  
    public void handleCmd(String x) throws IOException{
        if (x.contains("x+") || x.contains("x-") || x.contains("y+") || x.contains("y-")){
            try {
                JetsonServer.send(x);
                gui.display("Passed '" + x + "' to Arduino.");
                out.println("Passed '" + x + "' to Arduino.");
            } catch (Exception e){
                gui.display("Failed to send command: " + x + " to Arduino.");
                out.println("Failed to send command: " + x + " to Arduino.");
                System.exit(1);
            }
            } else if (x.equals("d")){
                //Close connection to client.
                out.println("Jetson will now disconnect...");
                gui.display("Jetson will now disconnect...");
                server.stop();
                gui.display("Disconnected from web client.");
            } else if (x.equals("aqon")){
                //Turn on Data Aquisition mode
                out.println("Entering Aquisition mode...");
                gui.display("Entering Aquisition mode...");
                executeCMD(aqon);
            } else if (x.equals("aqoff")){
                //Turn off Data Aquisition mode
                out.println("Stopping Aquisition mode...");
                gui.display("Stopping Aquisition mode...");
                executeCMD(aqoff);
            } else if (x.equals("deton")){
                //Turn on Detection mode
                out.println("Entering Detection mode...");
                gui.display("Entering Detection mode...");
                executeCMD(deton);
            } else if (x.equals("detoff")){
                //Turn off Detection mode
                out.println("Stopping Detection mode...");
                gui.display("Stopping Detection mode...");
                executeCMD(detoff);
            } else if (x.equals("test")){
                gui.display("'test' has been entered.");
            }
    }
}