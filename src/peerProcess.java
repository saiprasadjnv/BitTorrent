import java.io.*;
import java.util.Vector;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    static String HOMEDIR = System.getProperty("user.dir") + "/";
    //    static String HOMEDIR = "/Users/macuser/Documents/Study_1/Study/CN/Project/BitTorrent/";
    static String peerInfoConfig = "PeerInfo.cfg";
    static String commonConfig = "Common.cfg";
    public static Logger logger;
    private String peerHome;
    protected HashMap<String,RemotePeerInfo> peerInfoMap;
    protected Vector<RemotePeerInfo> peersToConnect;
    protected Vector<TCPConnectionInfo> activeConnections;
    protected HashMap<String, boolean[]> bitFieldsOfPeers;
    protected boolean[] myBitField;
    protected int numberOfPieces;
    //    protected MessageHandler myMessageHandler;
    FileObject myFileObject;
    /*
     * Constructor for the peerProcess object. Initializes the peerId.
     * */
    peerProcess(String peerId) {
        this.peerId = peerId;
        peerHome = HOMEDIR + "peer_" + peerId + "/";
        initializeLogger();
        getPeerInfo();
        initializeConfig();
        this.activeConnections = new Vector<TCPConnectionInfo>();
        this.messageQueue = new ConcurrentLinkedQueue<Message>();
        this.peersToTCPConnectionsMapping = new ConcurrentHashMap<String, TCPConnectionInfo>();
    }

    /*
     * Initializes Logger object for respective peer
     */
    private void initializeLogger() {
        try {
            logger = new Logger(this.peerId);
            logger.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Initialize the config parameters in the local datastructures. Read from the common.cfg file.
     * If the node does not have the target file, create a new file to read/write the shared pieces.
     * Initialize the bitfields of this node and all the peers in the P2P network.
     */
    void initializeConfig() {
        String st;
        try {
            BufferedReader in = new BufferedReader(new FileReader(commonConfig));
            while ((st = in.readLine()) != null) {

                String[] tokens = st.split("\\s+");
                switch (tokens[0]) {
                    case "NumberOfPreferredNeighbors":  // 2
                        this.numberOfPreferredNeighbours = Integer.parseInt(tokens[1]);
                        break;
                    case "UnchokingInterval":   // 5
                        this.unchokingInterval = Integer.parseInt(tokens[1]);
                        break;
                    case "OptimisticUnchokingInterval": // 15
                        this.optimisticUnchokingInterval = Integer.parseInt(tokens[1]);
                        break;
                    case "FileName":    // The File.dat
                        this.fileName = tokens[1];
                        break;
                    case "FileSize":    // 10
                        this.fileSize = Integer.parseInt(tokens[1]);
                        break;
                    case "PieceSize":   // 1
                        this.pieceSize = Integer.parseInt(tokens[1]);
                        break;
                    default:
                        throw new Exception("Invalid parameter in the file Common.cfg");
                }
            }
            bitFieldsOfPeers = new HashMap<String, boolean[]>();
            numberOfPieces = fileSize / pieceSize;
            if (fileSize % pieceSize != 0) {
                numberOfPieces++;
            }
            this.myBitField = new boolean[numberOfPieces];
            File peerDir = new File(peerHome);
            boolean mkdirRes = peerDir.mkdir();
            if (this.hasFile) {
                Arrays.fill(myBitField, true);
//                System.out.println("My bit field: " + " Size: " + myBitField.length);
//                Utility.printBooleanArray(myBitField);
            } else {
                Arrays.fill(myBitField, false);
                //Create an empty file to write pieces to.
                File newFile = new File(peerHome + fileName);
                boolean createNewFileRes = newFile.createNewFile();
            }
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /*
     * Get the information about all the peers in the network.
     * Select the peers for which the TCP connection needs to be initiated by this node.
     * Only the previously seen nodes in the peers list are requested for TCP connections.
     * All the nodes succeeding the current node in the PeerInfo.cfg file need to send connection request to this node.
     * */
    void getPeerInfo() {

        String st;
        peerInfoMap = new HashMap<String,RemotePeerInfo>();
        peersToConnect = new Vector<RemotePeerInfo>();
        boolean makeConnections = true;
        try {
            BufferedReader in = new BufferedReader(new FileReader(peerInfoConfig));
            while ((st = in.readLine()) != null) {
                String[] tokens = st.split("\\s+");
                RemotePeerInfo newNode = new RemotePeerInfo(tokens[0], tokens[1], tokens[2], Integer.parseInt(tokens[3]));
                if (newNode.peerId.equals(this.peerId)) {
                    this.listeningPort = Integer.parseInt(newNode.peerPort);
                    this.hasFile = newNode.hasFile;
                    makeConnections = false;
                } else {
                    peerInfoMap.put(newNode.peerId, newNode);
                }
                if (makeConnections) {
                    peersToConnect.addElement(newNode);
                }
            }
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /*
     * This is the starting point for a peer node. TCP connections are established here with all the nodes in the P2P network.
     * It then creates a Message handler to handle all the messages received by the node.
     * */
    public static void main(String[] args) {
        peerProcess peerNode = new peerProcess(args[0]);
        Runnable myMessageHandler = new MessageHandler(peerNode);
        new Thread(myMessageHandler).start();
        try {
            int remainingPeers = peerNode.peerInfoMap.size();
//            Send connection requests to all the peers listed in peersToConnect
            for (RemotePeerInfo node : peerNode.peersToConnect) {
                Socket requestSocket = new Socket(node.peerAddress, Integer.parseInt(node.peerPort));
                ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
                TCPConnectionInfo newTCPConnection = new TCPConnectionInfo(requestSocket, in, out, peerNode.peerId);
                Runnable newListenerThread = new ListenerThread(newTCPConnection, peerNode.messageQueue, peerNode.peersToTCPConnectionsMapping);
                new Thread(newListenerThread).start();
                remainingPeers--;
                peerNode.activeConnections.addElement(newTCPConnection);
                //Create a listener thread.
            }

            //Start a server on this node and wait for the remaining nodes to send connection requests
            ServerSocket listener = new ServerSocket(peerNode.listeningPort);
            while (remainingPeers > 0) {
                Socket listenSocket = listener.accept();
                ObjectOutputStream out = new ObjectOutputStream(listenSocket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(listenSocket.getInputStream());
                TCPConnectionInfo newTCPConnection = new TCPConnectionInfo(listenSocket, in, out, peerNode.peerId);
                Runnable newThread = new ListenerThread(newTCPConnection, peerNode.messageQueue, peerNode.peersToTCPConnectionsMapping);
                new Thread(newThread).start();
                peerNode.activeConnections.addElement(newTCPConnection);
                remainingPeers--;
//                System.out.println(remainingPeers);
            }
            listener.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        boolean isFileSharedToAllPeers = false;
        while (!isFileSharedToAllPeers) {
            //Wait until the peer sharing process is finished
            isFileSharedToAllPeers = true;
            for (boolean[] bitField : peerNode.bitFieldsOfPeers.values()) {
                for (boolean hasPiece : bitField) {
                    if (!hasPiece) {
                        isFileSharedToAllPeers = false;
                        break;
                    }
                }
            }
        }
        try {
            logger.stop();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
