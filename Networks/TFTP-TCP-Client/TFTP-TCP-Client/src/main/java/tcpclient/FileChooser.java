package tcpclient;

import javax.swing.*;
import java.io.File;

/**
 * Class that defines the popups to choose where files are retrieved from and stored
 */
public class FileChooser {

    /**
     * Method to show the user a popup where they can choose which file to upload to the server from their local drive
     * @return gets a string array where the first value is the absolute path of the file and the second is the file name
     */
    public static String[] getFile() {

        JFileChooser jFileChooser = new JFileChooser();

        int checkInput = jFileChooser.showOpenDialog(null);

        File openedFile = null;
        if (checkInput == JFileChooser.APPROVE_OPTION) {
            openedFile = jFileChooser.getSelectedFile();
            System.out.println("File not found");
        }
        return new String[]{openedFile.getAbsolutePath(), openedFile.getName()};


    }

    /**
     * Method to show the user a popup where they can choose which file to save the data into (or create a new file for this purpose) on their local drive
     * @return a file object with the file that has been chosen to save the data in
     */
    public static File saveFile() {

        JFileChooser jFileChooser = new JFileChooser();
        int checkInput = jFileChooser.showSaveDialog(null);
        if (checkInput == JFileChooser.APPROVE_OPTION){
        }
        return jFileChooser.getSelectedFile();
    }
}
