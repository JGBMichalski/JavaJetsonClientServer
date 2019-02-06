package jetsonserver;

import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class clientConnection{

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    static boolean isConnected = false;
    static String cmd = "";
    static gui gui;
    static clientConnection server;
    static JetsonServer JT;
    private String aqon;
    private String aqoff;
    private String deton1;
    private String deton2;
    private String detoff1;
    private String detoff2;
    
    

    public static void main(String[] args) {
        // Setting up clientConnection.java
        server = new clientConnection();
        server.loadVariables();
        
        // Setting up gui.java
        gui = new gui(server);
        gui.setVisible(true);
        
        // Setup JetsonServer.java and create its thread
        JT = new JetsonServer(gui);
        Thread t = new Thread(JT);
        t.start();
        //JT.connect();
        
        // Initiate the server to connect to web client
        server.start(8080);
        
        // If connected, listen for input from web client
        while (isConnected = true){
            cmd = server.listen();
        }
    }

    // Setup socket and wait for client
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
            gui.display("Could not setup socket. Please restart.");
        } 
        return clientSocket;
    }

    // Close all connections
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

    //Listen for input from web client
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
  
    // Execute any command sent from the web client
    public void handleCmd(String x) throws IOException{
        if (x.contains("x+") || x.contains("x-") || x.contains("y+") || x.contains("y-")){
            try {
                JT.send(x);
                gui.display("Passed '" + x + "' to Arduino.");
                //out.println("Passed '" + x + "' to Arduino.");
            } catch (Exception e){
                gui.display("Failed to send command: " + x + " to Arduino.");
                //out.println("Failed to send command: " + x + " to Arduino.");
                System.exit(1);
            }
        } else if (x.equals("d")){
            //Close connection to client.
            //out.println("Jetson will now disconnect...");
            gui.display("Jetson will now disconnect...");
            server.stop();
            gui.display("Disconnected from web client.");
        } else if (x.equals("aqon")){
            //Turn on Data Aquisition mode
            //out.println("Entering Aquisition mode...");
            gui.display("Entering Aquisition mode...");
            execLinCmd(aqon);
            JT.setMode(true);
        } else if (x.equals("aqoff")){
            //Turn off Data Aquisition mode
            //out.println("Stopping Aquisition mode...");
            gui.display("Stopping Aquisition mode...");
            execLinCmd(aqoff);
            JT.setMode(false);
        } else if (x.equals("deton")){
            //Turn on Detection mode
            //out.println("Entering Detection mode...");
            gui.display("Entering Detection mode...");
            execLinCmd(deton1);
            try{
                gui.display("Sleeping for 10 seconds to allow YOLO to boot...");
                //out.println("Sleeping for 10 seconds to allow YOLO to boot...");
                TimeUnit.SECONDS.sleep(10);
                gui.display("Wait completed.");
                //out.println("Wait completed.");
            } catch (Exception e){
                gui.display("Error during sleep.");
                //out.println("Error during sleep.");
            }
            //execLinCmd(deton2);
        } else if (x.equals("detoff")){
            //Turn off Detection mode
            //out.println("Stopping Detection mode...");
            gui.display("Stopping Detection mode...");
            execLinCmd(detoff1);
            execLinCmd(detoff2);
        } else {
            gui.display("Invalid command: " + x);
        }
    }
    
    // Execute any linux command sent by the web server
    private void execLinCmd(String cmd){
        String s = null;
        
        try {
            Process p = Runtime.getRuntime().exec(cmd);            
        }
        catch (Exception e) {
            gui.display("Error: " + e);
        }
    }
    
    // Load the variables for executing commands
    private void loadVariables(){
        // Launches Gstreamer and streams to browser on localhost
        aqon = "gst-launch-1.0 v4l2src device=/dev/video1 ! "
                    + "video/x-raw, width=3840, height=1080 ! videocrop top=0 left=0 "
                    + "right=1920 bottom=0 ! omxh264enc control-rate=2! tee name=t ! "
                    + "queue ! video/x-h264, stream-format=byte-stream ! h264parse ! "
                    + "rtph264pay ! udpsink host=localhost port=5000 t. ! queue";
        // Kills Gstreamer process
        aqoff = "killall -9 gstreamer";
        // Launches YOLOv3
        deton1 = "/home/nvidia/zed-yolo/zed_cpp_sample/build/darknet_zed "
                    + "/home/nvidia/zed-yolo/libdarknet/data/coco.names /home/nvidia/"
                    + "zed-yolo/libdarknet/cfg/yolov3-tiny.cfg /home/nvidia/zed-yolo/"
                    + "libdarknet/yolov3-tiny.weights";
        // Launches Gstreamer to stream YOLO to browser
        deton2 = "gst-launch-1.0 ximagesrc xname=\"ZED\" use-damage=0 ! "
                    + "video/x-raw ! timeoverlay ! queue ! videoconvert ! omxh264enc "
                    + "control-rate=2 ! tee name=t ! queue ! video/x-h264, "
                    + "stream-format=byte-stream ! h264parse ! rtph264pay ! "
                    + "udpsink host=140.193.230.117 port=5000 t. ! "
                    + "queue ! mpegtsmux ! filesink location=both.mp4 -e";
        // Kills YOLOv3 process
        detoff1= "killall -9 /home/nvidia/zed-yolo/zed_cpp_sample/build/darknet_zed";
        // Kills Gstreamer process
        detoff2= "killall -9 gstreamer";
    }
}