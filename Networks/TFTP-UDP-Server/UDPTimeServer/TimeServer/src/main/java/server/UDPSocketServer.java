package server;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * This is the server class that extends tread so that the server can be instanciated as a thread
 */
public class UDPSocketServer extends Thread {

    protected DatagramSocket socket;

    /**
     * This is the constructor for when a name argument is not given in which case it is called UDPSocketServer
     * @throws SocketException This is in case using a new datagram socket throws an exception
     */
    public UDPSocketServer() throws SocketException {
        this("UDPSocketServer");
    }

    /**
     *This is the constructor for when a name argument is given
     * @param name String of what to name the server
     * @throws SocketException This is in case using a new datagram socket throws an exception
     */
    public UDPSocketServer(String name) throws SocketException {
        super(name);

        socket = new DatagramSocket(9000);


    }

    /**
     * This is the method to run the thread which receives requests and deals with them accordingly
     */
    @Override
    public void run() {

        byte[] recvBuf = new byte[516];
        String directory = System.getProperty("user.dir") + "\\TFTP-UDP-Server\\UDPTimeServer\\TimeServer\\src\\main\\resources\\";
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(recvBuf, 516);
                socket.receive(packet);
                String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
                byte[] msgBytes = msg.getBytes();
                int opcode = msgBytes[0] + msgBytes[1];
                if (opcode == 1) {
                    int filenameSize = packet.getLength() - 9;
                    StringBuilder filename = new StringBuilder();
                    for (int i = 2; i < filenameSize + 2; i++) {
                        filename.append(msg.charAt(i));
                    }
                    String filepath = directory + filename;
                    if (filename.toString().equals("null")) {
                        System.out.println("File Name Empty");
                    } else {
                        InetAddress clientAddress = packet.getAddress();
                        int clientPort = packet.getPort();
                        FileInputStream inStream = null;
                        try {
                            File file = new File(filepath);
                            inStream = new FileInputStream(file);
                            byte[] data;
                            byte[] sizedData;
                            int bytesRead;
                            if (file.length() == 0) {
                                System.out.println("File Is Empty");
                            }
                            DatagramPacket[] packets;
                            if (file.length() % 512 == 0) {
                                int packetsAmount = (int) (file.length() / 512);
                                packets = new DatagramPacket[packetsAmount + 1];
                                for (int i = 1; i < packetsAmount + 1; i++) {
                                    data = new byte[516];
                                    bytesRead = inStream.read(data, 4, 512);
                                    data[1] = 2;
                                    data[3] = (byte) i;
                                    packet = new DatagramPacket(data, bytesRead + 4);
                                    packet.setAddress(clientAddress);
                                    packet.setPort(clientPort);
                                    packets[i - 1] = packet;
                                }
                                data = new byte[4];
                                data[1] = 2;
                                data[3] = (byte) (packetsAmount + 1);
                                packet = new DatagramPacket(data, 4);
                                packet.setAddress(clientAddress);
                                packet.setPort(clientPort);
                                packets[packetsAmount] = packet;

                            } else {
                                int packetsAmount = (int) Math.ceil(file.length() / 512.0);
                                packets = new DatagramPacket[packetsAmount];
                                for (int i = 1; i < packetsAmount + 1; i++) {
                                    data = new byte[516];
                                    bytesRead = inStream.read(data, 4, 512);
                                    sizedData = new byte[4 + bytesRead];
                                    System.arraycopy(data, 0, sizedData, 0, 4 + bytesRead);
                                    sizedData[1] = 2;
                                    sizedData[3] = (byte) i;
                                    packet = new DatagramPacket(sizedData, bytesRead + 4);
                                    packet.setAddress(clientAddress);
                                    packet.setPort(clientPort);
                                    packets[i - 1] = packet;
                                }
                            }

                            byte[] ack = new byte[4];
                            DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
                            socket.setSoTimeout(1000);
                            for (int i = 0; i < packets.length; i++) {
                                socket.send(packets[i]);
                                socket.receive(ackPacket);
                                ack = ackPacket.getData();
                                if (ack[0] == 0 && ack[1] == 4 && ack[2] == 0 && ack[3] == packets[i].getData()[3] && packets[i].getLength() == 516) {
                                } else if (ack[0] == 0 && ack[1] == 4 && ack[2] == 0 && ack[3] == packets[i].getData()[3] && packets[i].getLength() < 516) {
                                    System.out.println("File Sent");
                                } else {
                                    System.out.println("Not Properly Acknowledged");
                                    break;
                                }
                            }
                            socket.setSoTimeout(0);
                        } catch (FileNotFoundException e) {
                            System.err.println("File Not Found: " + filename + " " + e.getMessage());
                            String errorMessage = e.getMessage();
                            byte[] data = new byte[errorMessage.length()+5];
                            data[1] = (byte) 5;
                            data[3] = (byte) 1;
                            for (int i = 4; i< errorMessage.length()+4; i++){
                                data[i] = (byte) errorMessage.charAt(i-4);
                            }
                            data[errorMessage.length()+4] = (byte) 0;
                            DatagramPacket errorPacket = new DatagramPacket(data, data.length);
                            errorPacket.setAddress(clientAddress);
                            errorPacket.setPort(clientPort);
                            socket.send(errorPacket);

                        } finally {
                            if (inStream != null) {
                                try {
                                    inStream.close();
                                } catch (IOException e) {
                                    System.err.println("Input Stream Cannot Be Closed: " + filename + " " + e.getMessage());
                                }
                            }
                        }
                    }
                } else if (opcode == 2) {
                    int filenameSize = packet.getLength() - 9;
                    StringBuilder filename = new StringBuilder();
                    for (int i = 2; i < filenameSize + 2; i++) {
                        filename.append(msg.charAt(i));
                    }
                    File filepath = new File(directory + filename);
                    if (filename.toString().equals("null")) {
                        System.out.println("File Name Empty");
                    }
                    else{
                        InetAddress clientAddress = packet.getAddress();
                        int clientPort = packet.getPort();
                        System.out.println("Address: " + clientAddress);
                        System.out.println("Port: " + clientPort);
                        byte[] bytesOut = new byte[516];
                        DatagramPacket dataPacket = new DatagramPacket(bytesOut, 516);
                        boolean lastPacket = false;
                        FileOutputStream outstream = new FileOutputStream(filepath);
                        while (!lastPacket) {
                            socket.setSoTimeout(1000);
                            socket.receive(dataPacket);
                            if (!(dataPacket.getLength() == 516)) {
                                lastPacket = true;
                            }
                            outstream.write(dataPacket.getData(), 4, dataPacket.getLength() - 4);
                            packet.setLength(4);
                            byte[] ackData = new byte[4];
                            ackData[1] = 4;
                            ackData[3] = dataPacket.getData()[3];
                            packet.setData(ackData);
                            socket.send(packet);
                        }
                    }

                    packet.setLength(4);
                    byte[] ackData = new byte[4];
                    ackData[1] = 4;
                    ackData[3] = 1;
                    packet.setData(ackData);
                    socket.send(packet);
                    socket.setSoTimeout(0);
                    System.out.println("File Received");
                } else {
                    System.out.println("Invalid Opcode");
                }
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        socket.close();
    }

    /**
     * Main class to run the server
     * @param args no program arguments are used in the server, it is just obligatory for the main class
     * @throws IOException may be triggered with the input and output file streams but this theoretically shouldn't be possible with the way the server is set up
     */
    public static void main(String[] args) throws IOException {
        new UDPSocketServer().start();
        System.out.println("Server Started");
    }

}
