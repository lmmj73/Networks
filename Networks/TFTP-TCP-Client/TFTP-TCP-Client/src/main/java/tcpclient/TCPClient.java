package tcpclient;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

/**
 *
 */
public class TCPClient {

    // The client takes two user arguments as input: 1) Server IP address in dotted-decimal format, 2) Server TCP port
    // Change the default running configuration in Netbeans to pass the arguments.
    // the IP address can be the loopback address 127.0.0.1 and port number is 10000

    /**
     *
     * @param args The first program argument is the ip address and the second is the port number to send to
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Socket echoSocket;
        String address;
        int portNumber;
        String userInput;
        String serverResponse;


        DataOutputStream out;
        DataInputStream in;
        BufferedReader stdIn;
        Scanner scanner = new Scanner(System.in);
        int input = 0;

        if (args.length != 2) {
            System.err.println("Usage: java EchoClient <address> <port>");
            System.exit(1);
        }

        address = args[0];
        portNumber = Integer.parseInt(args[1]);

        System.out.println("Welcome");
        System.out.println("----------------------");
        System.out.println("(1) Store File");
        System.out.println("(2) Retrieve File");
        System.out.println("(3) Quit");
        System.out.println("----------------------");
        System.out.print("Please enter a number:");

        input = scanner.nextInt();

        byte[] buf = new byte[516];

        if(input == 1){
            System.out.println("----------------------");
            System.out.println("Please choose a file to store using the separate window. This may appear behind other open windows");
            String[] fileData = FileChooser.getFile();
            String path = fileData[0];
            String filename = fileData[1];
            buf = makePacket((byte) 2, filename);
            FileInputStream inStream = null;
            try {
                echoSocket = new Socket(address, portNumber);

                out = new DataOutputStream(echoSocket.getOutputStream());

                in = new DataInputStream(echoSocket.getInputStream());

                stdIn = new BufferedReader(new InputStreamReader(System.in));

                byte[] newbuf = new byte[516];

                int pointer = 0;
                newbuf[pointer] = (byte) 0;
                pointer++;
                newbuf[pointer] = (byte) 2;
                pointer++;
                for (int i = 0; i < filename.length(); i++) {
                    newbuf[pointer] = (byte) filename.charAt(i);
                    pointer++;
                }
                newbuf[pointer] = (byte) 0;
                pointer++;
                for (int i = 0; i < ("octet").length(); i++) {
                    newbuf[pointer] = (byte) ("octet").charAt(i);
                    pointer++;
                }

                out.write(newbuf);
                File file = new File(path);
                inStream = new FileInputStream(file);
                if (file.length() == 0) {
                    System.out.println("File Is Empty");
                }
                else if (file.length() % 512 == 0) {
                    int packetsAmount = (int) (file.length() / 512);
                    for (int i = 0; i < packetsAmount; i++) {
                        byte[] data = new byte[516];
                        int bytesRead = inStream.read(data, 4, 512);
                        data[1] = 2;
                        data[3] = (byte) i;
                        out.write(data);
                    }
                    byte[] data = new byte[4];
                    data[1] = 2;
                    data[3] = (byte) (packetsAmount);
                    out.write(data);
                } else {
                    int packetsAmount = (int) Math.ceil(file.length() / 512.0);
                    for (int i = 1; i < packetsAmount + 1; i++) {
                        byte[] data = new byte[516];
                        int bytesRead = inStream.read(data, 4, 512);
                        byte[] sizedData = new byte[4 + bytesRead];
                        System.arraycopy(data, 0, sizedData, 0, 4 + bytesRead);
                        sizedData[1] = 2;
                        sizedData[3] = (byte) i;
                        out.write(sizedData);
                    }
                }
                System.out.println("File Sent");
                inStream.close();
                out.close();

                echoSocket.close();
            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + address);
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " + address + ". Is the server running?");
                System.exit(1);
            }
        }
        else if(input == 2){
            try {
                echoSocket = new Socket(address, portNumber);


                out = new DataOutputStream(echoSocket.getOutputStream());

                in = new DataInputStream(echoSocket.getInputStream());


                System.out.println("----------------------");
                System.out.print("Please choose a file to read:");
                String fileName = scanner.next();
                System.out.println("----------------------");
                System.out.println("Please choose where the file is saved and name the file using the separate window. This may appear behind other open windows");
                File reconstructedFile = FileChooser.saveFile();
                buf = makePacket((byte) 1, fileName);
                //out.println(buf);
                out.write(buf);
                int size;
                byte[] data = new byte[516];
                FileOutputStream outstream = new FileOutputStream(reconstructedFile);
                while ((size = in.read(data)) > 0) {
                    outstream.write(data,4,size-4);
                    if(size < 512){
                        break;
                    }
                }
                System.out.println("File Received");
                outstream.close();
                out.close();
                echoSocket.close();
            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + address);
                System.exit(1);
            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection to " + address + ". Is the server running?");
                System.exit(1);
            }
        }
        else if(input == 3){
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
