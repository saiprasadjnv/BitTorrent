import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Runnable;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Listener thread associated with a TCP connection. Collects all the messages to the listener port.
 * Places the messages in message queue.
 */
public class ListenerThread implements Runnable {
    private TCPConnectionInfo monitorConnection;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private ConcurrentLinkedQueue<Message> messageQueue;
    private ConcurrentHashMap<String, TCPConnectionInfo> peersToTCPConnectionsMapping;

    public ListenerThread(TCPConnectionInfo monitorConnection, ConcurrentLinkedQueue<Message> messageQueue, ConcurrentHashMap<String, TCPConnectionInfo> peersToTCPConnectionsMapping) {
        this.monitorConnection = monitorConnection;
        this.outputStream = monitorConnection.out;
        this.inputStream = monitorConnection.in;
        this.messageQueue = messageQueue;
        this.peersToTCPConnectionsMapping = peersToTCPConnectionsMapping;
    }

    private boolean verifyHandshake(HandshakeMessage receivedHandshake) {
        if (!receivedHandshake.handshakeHeader.equals("P2PFILESHARINGPROJ") || this.peersToTCPConnectionsMapping.containsKey(receivedHandshake.peerID)) {
            return false;
        }
        return true;
    }

    public void run() {
        HandshakeMessage myHandshakeMessage = new HandshakeMessage(monitorConnection.myPeerID);
        try {
            this.outputStream.writeObject(myHandshakeMessage);
            this.outputStream.flush();
            HandshakeMessage receivedHandshake = (HandshakeMessage) this.inputStream.readObject();
            boolean isValidHandshake = verifyHandshake(receivedHandshake);
            if (isValidHandshake) {
                this.monitorConnection.associatedPeerId = receivedHandshake.peerID;
                this.peersToTCPConnectionsMapping.put(receivedHandshake.peerID, this.monitorConnection);
                Message newMessage = new Message();
                newMessage.messageOrigin = this.monitorConnection;
                messageQueue.add(newMessage);
            }
            while (!Thread.interrupted()) {
                Message newMessage = (Message) inputStream.readObject();
                newMessage.messageOrigin = this.monitorConnection;
                messageQueue.add(newMessage);
//                //System.out.print("InListenerOf;");
            }
        } catch (IOException | ClassNotFoundException ex) {
//            ex.printStackTrace();
        } catch (ClassCastException ex){
            //Ignore broken messages
        }
    }
}
