package tcpserver;


import java.net.*;
import java.io.*;

/**
 * Class that listens for any new requests and makes a new thread to deal with each one
 */
public class TCPServer {

    /**
     * This is the main class that sets up and starts the server, it also waits until data is sent to it and then creates a new thread to deal with it
     * @param args this is mandatory to have in the main class but no program arguments are used for the server
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        int portNumber = 10000;

        ServerSocket masterSocket;
        Socket slaveSocket;

        masterSocket = new ServerSocket(portNumber);


        System.out.println("Server Started...");

        while (true) {
            slaveSocket = masterSocket.accept();

            System.out.println("Accepted TCP connection from: " + slaveSocket.getInetAddress() + ", " + slaveSocket.getPort() + "...");
            System.out.println("Instantiating and starting new TCPServerThread object to handle the connection...");


            new TCPServerThread(slaveSocket).start();
        }
    }
}
