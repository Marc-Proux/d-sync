package src.syncing;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Server extends Network{
    
    public Server(int port, String path) {
        this.port = port;
        this.path = path;
        isServer = true;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started. Waiting for connection...");
            serverSocket.setSoTimeout(30000);
            socket = serverSocket.accept(); // attendra au maximum 30 secondes
            System.out.println("Client connected.");

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            is = socket.getInputStream();
            connect = true;

        } catch (IOException e) {
            System.err.println("Error setting up server: " + e.getMessage());
            connect = false;
        }
    }

    public void close() {
        try {
            socket.close();
            serverSocket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

    public void firstSync() throws IOException {
        List <DateAndName> listServer = listFiles(path, path);
        for (DateAndName file : listServer) {
            System.out.println(file.getName());
        }

        System.out.println("Waiting for files list...");
        try{
            List<DateAndName> listClient = receiveFilesList();
            System.out.println("Files list received.");

            System.out.println("Sending files list...");
            sendMessage(listServer);

            for (DateAndName fileServer : listServer) {
                if (fileServer.getType().equals("File")) {
                    Boolean contains = false;

                    for (DateAndName fileClient : listClient){
                        if (fileServer.getName().equals(fileClient.getName()) && fileClient.getType().equals("File")) {
                            contains = true;
                            if (fileServer.getDate() > fileClient.getDate()) {
                                System.out.println("Server send " + fileServer.getName() + "...");
                                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                                BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
                                sendFile(fileServer, oos, bos);
                                oos.reset();
                            }
                            else if(fileServer.getDate() < fileClient.getDate()) {
                                System.out.println("Server receive " + fileClient.getName() + "...");
                                // receiveFile();
                            }
                        }
                    }
                    if (!contains) {
                        System.out.println("Server send " + fileServer.getName() + "...");
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
                        sendFile(fileServer, oos, bos);
                        oos.reset();
                    }
                }
            }

            System.out.println("Server finished sending files.");
            System.out.println("Waiting for client to finish sending files...");

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());

            for (DateAndName fileClient : listClient) {
                if (fileClient.getType().equals("File")) {
                    // System.out.println("Receiving file " + fileClient.getName() + "...");
                    Boolean contains = false;

                    for (DateAndName fileServer : listServer){
                        if (fileClient.getName().equals(fileServer.getName()) && fileServer.getType().equals("File")) {
                            contains = true;
                            if (fileClient.getDate() > fileServer.getDate()) {
                                System.out.println("Client send " + fileClient.getName() + "...");
                                receiveFile(ois, bis);
                            }
                            else if(fileClient.getDate() < fileServer.getDate()) {
                                System.out.println("Client receive " + fileClient.getName() + "...");
                            }
                        }
                    }
                    if (!contains) {
                        System.out.println("Client send " + fileClient.getName() + "...");
                        receiveFile(ois, bis);
                    }
                }
            }
            System.out.println("Done.");
            ois.close();
            bis.close();
            
        } catch (ClassNotFoundException e) {
            System.err.println("Error receiving files list: " + e.getMessage());
        }

        
    }


    public static void main(String[] args) throws IOException{
        Server server = new Server(117, "/Users/marc/Library/CloudStorage/OneDrive-uha.fr/Cours/GitHub/Test");
        System.out.println(server.connect);
        server.firstSync();
        server.close();
    }
}
