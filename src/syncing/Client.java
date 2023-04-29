package src.syncing;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Client extends Network {
    public Client(String ip, int port, String path) {
        this.ip = ip;
        this.port = port;
        this.path = path;
        isServer = false;

        try{
            socket = new Socket(ip, port);
            System.out.println("Connected to server.");

            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            connect = true;

        } catch (IOException e) {
            System.err.println("Error setting up client: " + e.getMessage());
            connect = false;
        }
    }

    public void close() {
        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

    public void firstSync() throws IOException {
        File folder = new File(path);
        List<File> filesTemp = new ArrayList<File>(Arrays.asList(folder.listFiles()));
        List<DateAndName> files = new ArrayList<DateAndName>();

        for (File file : filesTemp) {
            String type;
            if (file.isFile()) {type = "File";}
            else {type = "Directory";}
            files.add(new DateAndName(file.getName(), file.lastModified(), type));
        }

        sendMessage(files);
    }

    public static void main(String[] args) throws IOException{
        Client server = new Client("192.168.1.55", 117, "C:/Users/skyec/Desktop/test");
        System.out.println(server.connect);
        server.firstSync();
        server.close();
    }
}
