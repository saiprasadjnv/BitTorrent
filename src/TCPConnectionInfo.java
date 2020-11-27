import java.net.*;
import java.io.*;

public class TCPConnectionInfo {
    public Socket connectedToSocket;
    public ObjectInputStream in;
    public ObjectOutputStream out;
    public String associatedPeerId;
    public String myPeerID;

    TCPConnectionInfo(Socket connectedToSocket, ObjectInputStream in, ObjectOutputStream out, String myPeerID) {
        this.connectedToSocket = connectedToSocket;
        this.in = in;
        this.out = out;
        this.myPeerID = myPeerID;
    }

    public void setAssociatedPeerId(String associatedPeerId) {
        this.associatedPeerId = associatedPeerId;
    }

    public String getAssociatedPeerId() {
        return associatedPeerId;
    }

    public void sendMessage(Message message) throws IOException {
        this.out.writeObject(message);
        this.out.flush();
    }

    public boolean isAlive() {
        return this.connectedToSocket.isConnected();
    }
}
