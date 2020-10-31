import java.io.BufferedReader;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.net.*;

public class peerProcess {
    private String peerId;
    private int numberOfPreferredNeighbours;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;
    private int listeningPort;
    private boolean hasFile;
//    static String peerInfoConfig = System.getProperty("user.dir") + "/PeerInfo.cfg";
//    static String commonConfig = System.getProperty("user.dir") + "/Common.cfg";
    static String peerInfoConfig = "/Users/macuser/Documents/Study_1/Study/CN/Project/BitTorrent/src/PeerInfo.cfg";
    static String commonConfig = "/Users/macuser/Documents/Study_1/Study/CN/Project/BitTorrent/src/Common.cfg";
    private Vector<RemotePeerInfo> peerInfoVector;
    private Vector<RemotePeerInfo> peersToConnect;
    private Vector<TCPConnectionInfo> activeConnections;

    /*
    * Constructor for the peerProcess object. Initializes the peerId.
    * */
    peerProcess(String peerId){
        this.peerId = peerId;
        initializeConfig();
        getPeerInfo();
        activeConnections = new Vector<TCPConnectionInfo>();
        //ToDo: Check if the peer has complete file or not and update hasFile.
    }


    /*
    * Initialize the config parameters in the local datastructures. Read from the common.cfg file.
    * */
    void initializeConfig(){
        String st;
        try {
            BufferedReader in = new BufferedReader(new FileReader(commonConfig));
            while((st = in.readLine()) != null) {

                String[] tokens = st.split("\\s+");
                if(tokens[0].equals("NumberOfPreferredNeighbors")){
                    this.numberOfPreferredNeighbours = Integer.parseInt(tokens[1]);
                }else if(tokens[0].equals("UnchokingInterval")){
                    this.unchokingInterval = Integer.parseInt(tokens[1]);
                }else if(tokens[0].equals("OptimisticUnchokingInterval")){
                    this.optimisticUnchokingInterval = Integer.parseInt(tokens[1]);
                }else if(tokens[0].equals("FileName")){
//                    System.out.println(tokens[0] + tokens[1]);
                    this.fileName = tokens[1];
                }else if(tokens[0].equals("FileSize")){
                    this.fileSize = Integer.parseInt(tokens[1]);
                }else if(tokens[0].equals("PieceSize")){
                    this.pieceSize = Integer.parseInt(tokens[1]);
                }else{
                     throw new Exception("Invalid parameter in the file Common.cfg");
                }
            }
            in.close();
        }
        catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    /*
    * Get the information about all the peers in the network.
    * Select the peers for which the TCP connection needs to be initiated by this node.
    * Only the previously seen nodes in the peers list are requested for TCP connections.
    * All the nodes succeeding the current node in the PeerInfo.cfg file need to send connection request to this node.
    * */
    void getPeerInfo(){

        String st;
        peerInfoVector = new Vector<RemotePeerInfo>();
        peersToConnect = new Vector<RemotePeerInfo>();
        boolean makeConnections = true;
        try {
            BufferedReader in = new BufferedReader(new FileReader(peerInfoConfig));
            while((st = in.readLine()) != null) {
                String[] tokens = st.split("\\s+");
                RemotePeerInfo newNode = new RemotePeerInfo(tokens[0], tokens[1], tokens[2]);
                System.out.println(newNode.peerId + " " +  this.peerId + " " + newNode.peerId.equals(this.peerId));
                if(newNode.peerId.equals(this.peerId)){
                    this.listeningPort = Integer.parseInt(newNode.peerPort);
                    makeConnections = false;
                }else{
                    peerInfoVector.addElement(newNode);
                }
                if(makeConnections){
                    peersToConnect.addElement(newNode);
                }
            }
            in.close();
        }
        catch (Exception ex) {
            // System.out.println(ex.toString());
            ex.printStackTrace();
        }
        System.out.println("Listener Port: " + this.listeningPort);
    }

    /*
     * This is the starting point for a peer node. TCP connections are established here with all the nodes in the P2P network.
     * It then creates a Message handler to handle all the messages received by the node.
     * */
    public static void main(String[] args){
//        System.out.println("Starting the peer process on peerId: " + args[0]);
        peerProcess peerNode = new peerProcess(args[0]);
//        System.out.println(peerNode.peersToConnect.toString());
//        for(RemotePeerInfo i: peerNode.peersToConnect){
//            System.out.println(i.peerId);
//        }
        try{
            int remainingPeers = peerNode.peerInfoVector.size();
            //Send connection requests to all the peers listed in peersToConnect
            for(RemotePeerInfo node: peerNode.peersToConnect){
                Socket requestSocket = new Socket(node.peerAddress, Integer.parseInt(node.peerPort));
                ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
                System.out.println("inputStream in main: " + in.hashCode());
                System.out.println("outStream in main: "+ out.hashCode());
                TCPConnectionInfo newTCPConnection = new TCPConnectionInfo(requestSocket, in, out, peerNode.peerId);
                Runnable newThread = new ListenerThread(newTCPConnection);
                new Thread(newThread).start();
                peerNode.activeConnections.addElement(newTCPConnection);
                //Create a listener thread.
            }

            //Start a server on this node and wait for the remaining nodes to send connection requests
            ServerSocket listener = new ServerSocket(peerNode.listeningPort);
//            System.out.println("Started listener on this node");
            while(remainingPeers>0){
                Socket listenSocket = listener.accept();
//                System.out.println("Received a connection request from a port id: " + listenSocket.getPort());
                ObjectOutputStream out = new ObjectOutputStream(listenSocket.getOutputStream());
//                System.out.println("Initialized the in and out buffers");
                out.flush();
                ObjectInputStream in = new ObjectInputStream(listenSocket.getInputStream());
//                System.out.println("Initialized the input buffer");
                System.out.println("inputStream in main: " + in.hashCode());
                System.out.println("outStream in main: "+ out.hashCode());
                TCPConnectionInfo newTCPConnection = new TCPConnectionInfo(listenSocket, in, out, peerNode.peerId);
//                System.out.println("Created new TCP connection object");
                Runnable newThread = new ListenerThread(newTCPConnection);
                new Thread(newThread).start();
                peerNode.activeConnections.addElement(newTCPConnection);
                // Create a listener thread for each connected node to collect messages from these nodes
                remainingPeers--;
//                System.out.println(remainingPeers);
            }
            listener.close();
        }catch (Exception ex){
//            System.out.println(ex.printStackTrace());
            ex.printStackTrace();
        }
        while(true){
        }

    }

}
