package tcpserver;

import java.net.*;
import java.io.*;
import java.util.Arrays;

/**
 * Class for the server thread which is run every time a new request is received and deals with it as necessary
 */
public class TCPServerThread extends Thread {

    private Socket slaveSocket = null;

    /**
     * This is the constructor for a new server thread
     *
     * @param socket takes the slave socket that this thread will be allocated to send and recieve through
     * */
    public TCPServerThread(Socket socket) {
        super("MTTCPServerThread");
        this.slaveSocket = socket;
    }

    /**
     * This is the function that, when a new request is received, deals with it and sends the correct response
     */
    @Override
    public void run() {
        String directory = System.getProperty("user.dir") + "\\TFTP-TCP-Server\\src\\main\\resources\\";
        DataOutputStream socketOutput;
        DataInputStream socketInput;
        try {
            socketOutput = new DataOutputStream(slaveSocket.getOutputStream());

            socketInput = new DataInputStream(slaveSocket.getInputStream());

            int bytesIn = 0;
            byte[] bytes = new byte[516];
            bytesIn = socketInput.read(bytes);
            int opcode = bytes[0]+bytes[1];

            if (opcode == 1) {
                int filenameSize = bytesIn - 9;
                StringBuilder filename = new StringBuilder();
                for (int i = 2; i < filenameSize + 2; i++) {
                    filename.append(((char) bytes[i]));
                }
                String filepath = directory + filename;
                if (filename.toString().equals("null")) {
                    System.out.println("File Name Empty");
                } else {
                    FileInputStream inStream = null;
                    try {
                        File file = new File(filepath);
                        inStream = new FileInputStream(file);
                        if (file.length() % 512 == 0) {
                            int packetsAmount = (int) (file.length() / 512);
                            for (int i = 0; i < packetsAmount; i++) {
                                byte[] data = new byte[516];
                                int bytesRead = inStream.read(data, 4, 512);
                                data[1] = 2;
                                data[3] = (byte) i;
                                socketOutput.write(data);
                            }
                            byte[] data = new byte[4];
                            data[1] = 2;
                            data[3] = (byte) (packetsAmount);
                            socketOutput.write(data);

                        } else {
                            int packetsAmount = (int) Math.ceil(file.length() / 512.0);
                            for (int i = 1; i < packetsAmount + 1; i++) {
                                byte[] data = new byte[516];
                                int bytesRead = inStream.read(data, 4, 512);
                                byte[] sizedData = new byte[4 + bytesRead];
                                System.arraycopy(data, 0, sizedData, 0, 4 + bytesRead);
                                sizedData[1] = 2;
                                sizedData[3] = (byte) i;
                                socketOutput.write(sizedData);
                            }
                        }
                        inStream.close();
                        socketOutput.close();
                        socketInput.close();

                        System.out.println("File Sent");
                    } catch (FileNotFoundException e) {
                    }
                }
            }
            else if(opcode == 2) {
                StringBuilder filename = new StringBuilder();
                int j = 2;
                while(bytes[j] != 0){
                    filename.append(((char) bytes[j]));
                    j++;
                }

                String filepath = directory + filename;
                System.out.println("FilePath: "+ filepath);
                FileOutputStream outstream = new FileOutputStream(filepath);
                if (filename.toString().equals("null")) {
                    System.out.println("File Name Empty");
                } else {
                    int size;
                    byte[] data = new byte[516];

                    while ((size = socketInput.read(data)) > 0) {
                        outstream.write(data, 4, size-4);
                        if (size < 512) {
                            break;
                        }

                    }
                    outstream.close();
                    socketInput.close();
                    socketOutput.close();
                    slaveSocket.close();
                }
                System.out.println("File Received");

            }
            else{
                System.out.println("Invalid Opcode");
            }


        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
