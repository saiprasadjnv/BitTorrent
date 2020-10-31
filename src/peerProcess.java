import java.io.BufferedReader;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class peerProcess {
    protected String peerId;
    protected int numberOfPreferredNeighbours;
    protected int unchokingInterval;
    protected int optimisticUnchokingInterval;
    protected String fileName;
    protected int fileSize;
    protected int pieceSize;
    protected int listeningPort;
    protected boolean hasFile;
    public ConcurrentLinkedQueue<Message> messageQueue;
    public ConcurrentHashMap<String, TCPConnectionInfo> peersToTCPConnectionsMapping;
    static String peerInfoConfig = "/Users/macuser/Documents/Study_1/Study/CN/Project/BitTorrent/src/PeerInfo.cfg";
    static String commonConfig = "/Users/macuser/Documents/Study_1/Study/CN/Project/BitTorrent/src/Common.cfg";
    protected Vector<RemotePeerInfo> peerInfoVector;
    protected Vector<RemotePeerInfo> peersToConnect;
    protected Vector<TCPConnectionInfo> activeConnections;
    protected HashMap<String, boolean[]> bitFieldsOfPeers;
    protected boolean[] myBitField;
    private int numberOfPieces;
    MessageHandler myMessageHandler;
    /*
    * Constructor for the peerProcess object. Initializes the peerId.
    * */
    peerProcess(String peerId){
        this.peerId = peerId;
        initializeConfig();
        getPeerInfo();
        this.activeConnections = new Vector<TCPConnectionInfo>();
        this.messageQueue = new ConcurrentLinkedQueue<Message>();
        this.peersToTCPConnectionsMapping = new ConcurrentHashMap<String, TCPConnectionInfo>();
        this.myMessageHandler = new MessageHandler(this);
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
                switch (tokens[0]) {
                    case "NumberOfPreferredNeighbors":
                        this.numberOfPreferredNeighbours = Integer.parseInt(tokens[1]);
                        break;
                    case "UnchokingInterval":
                        this.unchokingInterval = Integer.parseInt(tokens[1]);
                        break;
                    case "OptimisticUnchokingInterval":
                        this.optimisticUnchokingInterval = Integer.parseInt(tokens[1]);
                        break;
                    case "FileName":
//                    System.out.println(tokens[0] + tokens[1]);
                        this.fileName = tokens[1];
                        break;
                    case "FileSize":
                        this.fileSize = Integer.parseInt(tokens[1]);
                        break;
                    case "PieceSize":
                        this.pieceSize = Integer.parseInt(tokens[1]);
                        break;
                    default:
                        throw new Exception("Invalid parameter in the file Common.cfg");
                }
            }
            in.close();
        }
        catch (Exception ex) {
            System.out.println(ex.toString());
        }
        this.numberOfPieces = fileSize/pieceSize + (fileSize%pieceSize);
        this.myBitField = new boolean[numberOfPieces];
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
//                System.out.println(newNode.peerId + " " +  this.peerId + " " + newNode.peerId.equals(this.peerId));
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
            ex.printStackTrace();
        }
//        System.out.println("Listener Port: " + this.listeningPort);
    }

    /*
     * This is the starting point for a peer node. TCP connections are established here with all the nodes in the P2P network.
     * It then creates a Message handler to handle all the messages received by the node.
     * */
    public static void main(String[] args){
        peerProcess peerNode = new peerProcess(args[0]);
        try{
            int remainingPeers = peerNode.peerInfoVector.size();
            //Send connection requests to all the peers listed in peersToConnect
            for(RemotePeerInfo node: peerNode.peersToConnect){
                Socket requestSocket = new Socket(node.peerAddress, Integer.parseInt(node.peerPort));
                ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
                TCPConnectionInfo newTCPConnection = new TCPConnectionInfo(requestSocket, in, out, peerNode.peerId);
                Runnable newThread = new ListenerThread(newTCPConnection, peerNode.messageQueue, peerNode.peersToTCPConnectionsMapping);
                new Thread(newThread).start();
                remainingPeers--;
                peerNode.activeConnections.addElement(newTCPConnection);
                //Create a listener thread.
            }

            //Start a server on this node and wait for the remaining nodes to send connection requests
            ServerSocket listener = new ServerSocket(peerNode.listeningPort);
            System.out.println("Started listener on this node");
            System.out.println("Remaining peers: " + remainingPeers);
            while(remainingPeers>0){
                Socket listenSocket = listener.accept();
                System.out.println("Received a connection request from a port id: " + listenSocket.getPort());
                ObjectOutputStream out = new ObjectOutputStream(listenSocket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(listenSocket.getInputStream());
                TCPConnectionInfo newTCPConnection = new TCPConnectionInfo(listenSocket, in, out, peerNode.peerId);
                Runnable newThread = new ListenerThread(newTCPConnection, peerNode.messageQueue, peerNode.peersToTCPConnectionsMapping);
                new Thread(newThread).start();
                peerNode.activeConnections.addElement(newTCPConnection);
                remainingPeers--;
                System.out.println(remainingPeers);
            }

            listener.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }

        while(true){
            System.out.println(peerNode.peersToTCPConnectionsMapping.toString());
        }

    }

}
