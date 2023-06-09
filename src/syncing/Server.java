package src.syncing;

import java.io.*;
import java.util.List;

/**
 * Server class that extends Network. It is used to create a server that will be able to send and receive files.
 * See {@link src.syncing.Network} class for more information.
 * <br/>
 * Author: Marc Proux
 */
public class Server extends Network{
    
    /**
     * Constructor of the Server class.
     * 
     * @param port The port on which the server will be listening.
     * @param path The path of the folder that will be synchronized.
     * @throws Exception
     */
    public Server(int port, String path) throws Exception{
        this.port = port;
        this.path = path;
        isServer = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void firstSync() throws IOException {
        listServer = listFiles(path, path);

        try{
            List<DateAndName> listClient = (List<DateAndName>) receiveMessage();

            sendMessage(listServer);

            resetConnection();

            for (DateAndName fileServer : listServer) {
                Boolean contains = false;

                for (DateAndName fileClient : listClient){
                    if (fileServer.getName().equals(fileClient.getName()) && fileClient.getType().equals("File")) {
                        contains = true;
                        if (fileServer.getDate() > fileClient.getDate()) {
                            sendFile(fileServer);
                        }
                    }
                }
                if (!contains) {
                    if (fileServer.getType().equals("File")) {
                        sendFile(fileServer);
                    }
                }
            }

            for (DateAndName fileClient : listClient) {
                Boolean contains = false;

                for (DateAndName fileServer : listServer){
                    if (fileServer.getName().equals(fileClient.getName()) && fileServer.getType().equals("File")) {
                        contains = true;
                        if (fileClient.getDate() > fileServer.getDate()) {
                            receiveFile(fileClient);
                        }
                    }
                }
                if (!contains) {
                    if (fileClient.getType().equals("File")) {
                        receiveFile(fileClient);
                    }
                    else {
                        File folder = new File(path + "/" + fileClient.getPath());
                        folder.mkdirs();
                    }
                }
            }
            lastState = listFiles(path, path);
            addMessage("Folders combined.");
            firstSync = false;
            
        } catch (ClassNotFoundException e) {
            System.err.println("Error receiving files list: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void syncAndDelete() throws IOException{
        isChange = false;
        List <DateAndName> listServer = listFiles(path, path);

        try {
            List<DateAndName> listClient = (List<DateAndName>) receiveMessage();

            sendMessage(listServer);

            resetConnection();

            for (DateAndName fileClient : listClient){
                DateAndName fileServer = listServer.stream().filter(o -> o.getPath().equals(fileClient.getPath()) && o.getType().equals(o.getType())).findFirst().orElse(null);
                if (fileServer != null){
                    if (fileClient.getDate() > fileServer.getDate() && fileClient.getType().equals("File")) {
                        receiveFile(fileClient);
                        addMessage("Copied the modified file.");
                        isChange = true;
                    }
                } else {
                    isChange = true;
                    if (!lastState.stream().anyMatch(o -> o.getType().equals(fileClient.getType()) && o.getPath().equals(fileClient.getPath()))){
                        if (fileClient.getType().equals("File")) {
                            receiveFile(fileClient);
                            addMessage("Copied " + fileClient.getName() + ".");
                        }
                        else{
                            createDirectory(fileClient);
                            addMessage("Copied " + fileClient.getName() + ".");
                        }
                    }
                }
            }

            for (DateAndName fileServer : listServer){
                DateAndName fileClient = listClient.stream().filter(o -> o.getPath().equals(fileServer.getPath()) && o.getType().equals(fileServer.getType())).findFirst().orElse(null);
                if (fileClient != null){
                    if (fileServer.getDate() > fileClient.getDate() && fileClient.getType().equals("File")) {
                        sendFile(fileServer);
                        isChange = true;
                    }
                } else {
                    isChange = true;
                    if (lastState.stream().anyMatch(o -> o.getType().equals(fileServer.getType()) && o.getPath().equals(fileServer.getPath()))){
                        deleteFile(fileServer);
                        addMessage("Deleted " + fileServer.getName() + ".");
                    } else {
                        if (fileServer.getType().equals("File")) {
                            sendFile(fileServer);
                        }
                    }
                }
            }

            lastState.clear();
            lastState = listFiles(path, path);
            if (isChange) {
                addMessage("Folders synced.");
            }
            else {
                if (!messages.get(7).equals("No changes detected") && !messages.get(7).equals("No changes detected.") && !messages.get(7).equals("No changes detected..") && !messages.get(7).equals("No changes detected...") ) {
                    addMessage("No changes detected");
                }
                else if (messages.get(7).equals("No changes detected")) {
                    messages.set(7, "No changes detected.");
                }
                else if (messages.get(7).equals("No changes detected.")) {
                    messages.set(7, "No changes detected..");
                }
                else if (messages.get(7).equals("No changes detected..")) {
                    messages.set(7, "No changes detected...");
                }
                else if (messages.get(7).equals("No changes detected...")) {
                    messages.set(7, "No changes detected");
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Error receiving files list: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(){
        while(true){
            if(active){
                try{
                    connect();
                    while(connect){
                        File folder = new File(path);

                        Boolean state[] = {folder.exists() && folder.isDirectory(), syncCurrent, firstSync};
                        sendMessage(state);
                        Boolean state2[] = (Boolean[]) receiveMessage();

                        Boolean foldesrExist = state2[0] && state[0];
                        sync = state2[1] && state[1];
                        Boolean firstSyncAll = state2[2] || state[2];

                        resetConnection();

                        if(!sync && syncCurrent){
                            if (!messages.get(7).equals("Waiting for client to resume syncing.")) {
                                addMessage("Waiting for client to resume syncing.");
                            }
                        }
                        else if(!syncCurrent){
                            if (!messages.get(7).equals("Syncing Paused.")) {
                                addMessage("Syncing Paused.");
                            }
                        }
                        else if (firstSyncAll && sync && foldesrExist){
                            firstSync();
                        }
                        else if (sync && foldesrExist){
                            syncAndDelete();
                        }
                        else if(!foldesrExist && folder.exists()){
                            if (!messages.get(7).equals("A problem as occured on the client's side.")) {
                                addMessage("A problem as occured on the client's side.");
                            }
                        }
                        else if(!folder.exists()){
                            if (!messages.get(7).equals("The local folder path is'nt valid anymore.")) {
                                addMessage("The local folder path is'nt valid anymore.");
                            }
                        }
                        Thread.sleep(2000);
                    }

                }
                catch(Exception e){
                    if(!messages.get(6).equals("Connection lost.")){
                        addMessage("Connection lost.");
                    }
                    System.out.println("Error in run :"+e);
                    try{
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        System.out.println("Error while waiting: " + ie.getMessage());
                    }
                }
            }
            try{
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                System.out.println("Error while waiting: " + ie.getMessage());
            }
        }
    }
}
