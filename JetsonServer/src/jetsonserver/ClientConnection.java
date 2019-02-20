package jetsonserver;

import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class ClientConnection implements Runnable{

    private DatagramSocket serverSocket; //ServerSocket type for TCP, DatagramPacket for UDP
    private DatagramPacket receivePacket;
    private InetAddress IPAddress;
    private String sentence;
    private DatagramPacket sendPacket ;
    private int port = 8080;
    private byte[] received;
    private byte[] sent;
    static boolean isConnected = false;
    static String cmd = "";
    static GUI gui;
    static ClientConnection server;
    static JetsonServer JT;
    private String aqon;
    private String aqoff;
    private String deton1;
    private String deton2;
    private String detoff1;
    private String detoff2;
    
    

    public static void main(String[] args) {
        // Setting up clientConnection.java
        server = new ClientConnection();
        server.loadVariables();
        
        // Setting up gui.java
        gui = new GUI(server);
        gui.setVisible(true);
        
        // Setup JetsonServer.java and create its thread
        JT = new JetsonServer(gui);
        Thread t = new Thread(JT);
        t.start();
        JT.connect();
        
        // Initiate the server to connect to web client
        server.start(8080);
        Thread h = new Thread(server);
        h.start();
    }

    // Setup socket and wait for client
    public void start(int port){
        try {
            gui.display("Setting up socket...");
            serverSocket = new DatagramSocket(port);
            gui.display("Socket Setup...");
            isConnected = true;
        } catch (IOException e){
            gui.display("Could not setup socket. Please restart.");
        } 
    }

    // Close all connections
    public void stop(){
    	isConnected = false;
        serverSocket.close();
        gui.display("Server stopped.");
    }

    //Listen for input from web client
    public void run(){
    	while (isConnected){
	        try {
	        	receivePacket = new DatagramPacket(received, received.length);
	            serverSocket.receive(receivePacket);
	            sentence = new String( receivePacket.getData());
	            gui.display("RECEIVED: " + sentence);
	            handleCmd(sentence);
	        } catch (IOException e){
	            e.printStackTrace();
	        }
    	}
    }
    
    public String send(String x) throws IOException {
    	if (receivePacket.getAddress() != null) {
	    	IPAddress = receivePacket.getAddress();
	        port = receivePacket.getPort();
	        sent = x.getBytes();
	        sendPacket = new DatagramPacket(sent, sent.length, IPAddress, port);
	        serverSocket.send(sendPacket);
	        return sentence;
    	} else {
    		return null;
    	}
    }
  
    // Execute any command sent from the web client
    public void handleCmd(String x) throws IOException{
        if (x.contains("x+") || x.contains("x-") || x.contains("y+") || x.contains("y-")){
            try {
                JT.send(x);
                gui.display("Passed '" + x + "' to Arduino.");
                //send("Passed '" + x + "' to Arduino.");
            } catch (Exception e){
                gui.display("Failed to send command: " + x + " to Arduino.");
                //send("Failed to send command: " + x + " to Arduino.");
                System.exit(1);
            }
        } else if (x.contains("aqon")){
            //Turn on Data Acquisition mode
            send("Entering Acquisition mode...");
            gui.display("Entering Acquisition mode...");
            execLinCmd(aqon);
            JT.setMode(true);
        } else if (x.contains("aqoff")){
            //Turn off Data Acquisition mode
        	send("Stopping Acquisition mode...");
            gui.display("Stopping Acquisition mode...");
            execLinCmd(aqoff);
            JT.setMode(false);
        } else if (x.contains("deton")){
            //Turn on Detection mode
        	//send("Entering Detection mode...");
            gui.display("Entering Detection mode...");
            execLinCmd(deton1);
            try{
                gui.display("Sleeping for 10 seconds to allow YOLO to boot...");
                //send("Sleeping for 10 seconds to allow YOLO to boot...");
                TimeUnit.SECONDS.sleep(10);
                gui.display("Wait completed.");
                //send("Wait completed.");
            } catch (Exception e){
                gui.display("Error during sleep.");
                //send("Error during sleep.");
            }
            execLinCmd(deton2);
        } else if (x.contains("detoff")){
            //Turn off Detection mode
        	//send("Stopping Detection mode...");
            gui.display("Stopping Detection mode...");
            execLinCmd(detoff1);
            execLinCmd(detoff2);
        } else if (x.contains("senton")){
        	JT.setMode(true);
        	//send("Sentry Mode Enabled.");
        	gui.display("Sentry Mode enabled.");
        } else if (x.contains("sentoff")){
        	JT.setMode(false);
        	//send("Sentry Mode Disabled.");
        	gui.display("Sentry Mode disabled.");
        } else if (x.contains("d")){
            //Close connection to client.
            send("Jetson will now disconnect...");
            gui.display("Jetson will now disconnect...");
            server.stop();
            server.start(8080);
            gui.display("Disconnected from web client.");
        } else {
            //JT.setTime(Integer.parseInt(x));
        	gui.display("Invalid command: " + x);
        }
    }
    
    // Execute any Linux command sent by the web server
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
        aqon = "gst-launch-1.0 v4l2src device=/dev/video1 ! video/x-raw, width=3840, "
        		+ "height=1080, framerate=30/1 ! videocrop top=0 left=0 right=1920 "
        		+ "bottom=0 ! videoconvert ! videoscale ! video/x-raw,width=360,height=240 "
        		+ "! clockoverlay shaded-background=true font-desc=\"Sans 24\" ! "
        		+ "vp8enc target-bitrate=2500000 ! webmmux streamable=true ! queue "
        		+ "! tcpserversink host=127.0.0.1 port=5000";
        		//"gst-launch-1.0 v4l2src device=/dev/video1 ! video/x-raw, width=3840,"
        		//+ " height=1080 ! videocrop top=0 left=0 right=1920 bottom=0 ! "
        		//+ "videoconvert ! videoscale ! video/x-raw,width=720,height=360 ! "
        		//+ "clockoverlay shaded-background=true font-desc=\"Sans 24\" ! "
        		//+ "theoraenc ! oggmux ! tcpserversink host=127.0.0.1 port=5000";
        // Kills Gstreamer process
        aqoff = "killall -9 gst-launch-1.0";
        // Launches YOLOv3
        deton1 = "/home/nvidia/zed-yolo/zed_cpp_sample/build/darknet_zed "
                    + "/home/nvidia/zed-yolo/libdarknet/data/coco.names /home/nvidia/"
                    + "zed-yolo/libdarknet/cfg/yolov3-tiny.cfg /home/nvidia/zed-yolo/"
                    + "libdarknet/yolov3-tiny.weights";
        // Launches Gstreamer to stream YOLO to browser
        deton2 = "gst-launch-1.0 ximagesrc xname=\"ZED\" use-damage=0 !"
        		+ " videoconvert ! videoscale ! video/x-raw,width=360,height=240 "
        		+ "! clockoverlay shaded-background=true font-desc=\"Sans 24\" ! "
        		+ "vp8enc target-bitrate=2500000 ! webmmux streamable=true ! queue "
        		+ "! tcpserversink host=127.0.0.1 port=5000";
        		//"gst-launch-1.0  ximagesrc xname=\"ZED\" use-damage=0 ! videoconvert"
        		//+ " ! videoscale ! video/x-raw,width=720,height=360 ! clockoverlay "
        		//+ "shaded-background=true font-desc=\"Sans 24\" ! theoraenc ! oggmux"
        		//+ " ! tcpserversink host=127.0.0.1 port=5000";
        // Kills YOLOv3 process
        detoff1= "killall -9 /home/nvidia/zed-yolo/zed_cpp_sample/build/darknet_zed";
        // Kills Gstreamer process
        detoff2= "killall -9 gst-launch-1.0";
        
        sent = new byte[1024];
        received = new byte[1024];
    }
}