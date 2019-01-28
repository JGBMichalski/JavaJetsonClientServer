package jetsonserver;

import java.io.*;
import java.net.*;

public class clientConnection{

  private ServerSocket serverSocket;
  private Socket clientSocket;
  private PrintWriter out;
  private BufferedReader in;

  static boolean isConnected = false;
  static String cmd = "";
  static gui gui;
  static clientConnection server;

  public static void main(String[] args) {
    gui = new gui();
    gui.setVisible(true);
    server = new clientConnection();
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
  
  private void handleCmd(String x) throws IOException{
        if (x.contains("x+") || x.contains("x-") || x.contains("y+") || x.contains("y-")){
            try {
                JetsonServer.send(x);
                gui.display("Passed '" + x + "' to Arduino.");
                out.println("Passed '" + x + "' to Arduino.");
            } catch (Exception e){
                gui.display("Failed to send command: " + x + "to Arduino.");
                out.println("Failed to send command: " + x + "to Arduino.");
                System.exit(1);
            }
        } else if (x.equals("d")){
                //Close connection to client.
                out.println("Jetson will now disconnect...");
                gui.display("Jetson will now disconnect...");
                server.stop();
                gui.display("Disconnected from web client.");
        }
    }
}