package client;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

/**
 * Creates the client that can store and receive files from the server
 */
public class UDPSocketClient {

    /**
     * Main class to run the server
     * @param args The first and only program argument is the ip address of the server to send to
     * @throws IOException This is for the file input and output streams
     */
    public static void main(String[] args) throws IOException {

        DatagramSocket socket;
        DatagramPacket packet;
        DatagramPacket dataPacket;
        Scanner scanner = new Scanner(System.in);
        int input;
        if (args.length != 1) {
            System.out.println("the hostname of the server is required");
            return;
        }
        System.out.println("Welcome");
        System.out.println("----------------------");
        System.out.println("(1) Store File");
        System.out.println("(2) Retrieve File");
        System.out.println("(3) Quit");
        System.out.println("----------------------");
        System.out.print("Please enter a number:");
        byte[] buf;
        socket = new DatagramSocket(4000);
        InetAddress address = InetAddress.getByName(args[0]);

        input = scanner.nextInt();

        if (input == 1) {
            System.out.println("----------------------");
            System.out.println("Please choose a file to store using the separate window. This may appear behind other open windows");
            String[] fileData = FileChooser.getFile();
            String path = fileData[0];
            String filename = fileData[1];
            buf = makePacket((byte) 2, filename);
            packet = new DatagramPacket(buf, buf.length);
            packet.setAddress(address);
            packet.setPort(9000);
            socket.send(packet);

            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();
            System.out.println("Address: " + clientAddress);
            System.out.println("Port: " + clientPort);
            FileInputStream inStream = null;
            try {
                File file = new File(path);
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
        else if (input == 2) {
            System.out.println("----------------------");
            System.out.print("Please choose a file to read, these are the ones stored in the resources folder of the server:");

            String fileName = scanner.next();

            System.out.println("----------------------");
            System.out.println("Please choose where the file is saved and name the file using the separate window. This may appear behind other open windows");
            File reconstructedFile = FileChooser.saveFile();
            buf = makePacket((byte) 1, fileName);

            packet = new DatagramPacket(buf, buf.length);
            packet.setAddress(address);
            packet.setPort(9000);


            socket.send(packet);
            byte[] bytesOut = new byte[516];
            dataPacket = new DatagramPacket(bytesOut, 516);
            boolean lastPacket = false;
            FileOutputStream outstream = new FileOutputStream(reconstructedFile);
            socket.setSoTimeout(1000);
            while (!lastPacket) {
                socket.receive(dataPacket);
                if (dataPacket.getData()[1] == (byte) 5){
                    StringBuilder errorMessage = new StringBuilder();
                    for (int i = 4; i< dataPacket.getLength()-1; i++){
                        errorMessage.append((char)dataPacket.getData()[i]);
                    }
                    System.out.println(errorMessage);
                    break;
                }
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
            socket.setSoTimeout(0);
            System.out.println("File Received");
            outstream.close();
        }
        else if (input == 3) {
            socket.close();
            System.exit(1);
        }
        else{
            System.out.println("Option not recognised");
        }

    }

    /**
     * Makes a new read or write request byte array that can be put in a packet and sent
     * @param code the opcode for the request (1 is read, 2 is write)
     * @param file The name of the file that is being read or written
     * @return a byte array formatted to be either a read or write request to the server
     */
    private static byte[] makePacket(byte code, String file) {
        byte zeroByte = 0;
        int len = 4 + file.length() + ("octet").length();
        byte[] arr = new byte[len];

        int pointer = 0;
        arr[pointer] = zeroByte;
        pointer++;
        arr[pointer] = code;
        pointer++;
        for (int i = 0; i < file.length(); i++) {
            arr[pointer] = (byte) file.charAt(i);
            pointer++;
        }
        arr[pointer] = zeroByte;
        pointer++;
        for (int i = 0; i < ("octet").length(); i++) {
            arr[pointer] = (byte) ("octet").charAt(i);
            pointer++;
        }
        arr[pointer] = zeroByte;
        return arr;
    }
}