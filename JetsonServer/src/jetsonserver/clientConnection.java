package jetsonserver;

import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class clientConnection{

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String aqon = "gst-launch-1.0 v4l2src device=/dev/video1 ! "
            + "video/x-raw, width=3840, height=1080 ! videocrop top=0 left=0 "
            + "right=1920 bottom=0 ! omxh264enc control-rate=2! tee name=t ! "
            + "queue ! video/x-h264, stream-format=byte-stream ! h264parse ! "
            + "rtph264pay ! udpsink host=140.193.207.255 port=5000 t. ! queue";
    private String aqoff = "";
    private String deton1 = "./darknet_zed ../../libdarknet/data/coco.names "
            + "../../libdarknet/cfg/yolov3-tiny.cfg yolov3-tiny.weights";
    private String deton2 = "gst-launch-1.0 ximagesrc xname=\"ZED\" use-damage=0 ! "
            + "video/x-raw ! timeoverlay ! queue ! videoconvert ! omxh264enc "
            + "control-rate=2 ! tee name=t ! queue ! video/x-h264, "
            + "stream-format=byte-stream ! h264parse ! rtph264pay ! "
            + "udpsink host=140.193.230.117 port=5000 t. ! "
            + "queue ! mpegtsmux ! filesink location=both.mp4 -e";
    private String detoff= "";

    static boolean isConnected = false;
    static String cmd = "";
    static gui gui;
    static clientConnection server;

    public static void main(String[] args) {
        server = new clientConnection();
        gui = new gui(server);
        gui.setVisible(true);
        JetsonServer.connect();
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
        } else if (x.equals("aqoff")){
            //Turn off Data Aquisition mode
            //out.println("Stopping Aquisition mode...");
            gui.display("Stopping Aquisition mode...");
            execLinCmd(aqoff);
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
            execLinCmd(detoff);
        } else if (x.equals("test")){
            gui.display("'test' has been entered.");
        }
    }
    
    private void execLinCmd(String cmd){
        String s = null;
        
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // read the output from the command
            gui.display("Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                gui.display(s);
            }
            
            // read any errors from the attempted command
            gui.display("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                gui.display(s);
            }
            
            stdInput.close();
            stdError.close();
            
        }
        catch (Exception e) {
            gui.display("Error: " + e);
        }
    }
}