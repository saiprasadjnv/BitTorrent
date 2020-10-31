import java.net.*;
import java.io.*;
public class TCPConnectionInfo {
    public Socket connectedToSocket;
    public ObjectInputStream in;
    public ObjectOutputStream out;
    public String associatedPeerId;
    public String myPeerID;

    public void setAssociatedPeerId(String associatedPeerId) {
        this.associatedPeerId = associatedPeerId;
    }

    public String getAssociatedPeerId() {
        return associatedPeerId;
    }

    TCPConnectionInfo(Socket connectedToSocket, ObjectInputStream in, ObjectOutputStream out, String myPeerID){
        this.connectedToSocket = connectedToSocket;
        this.in = in;
        this.out = out;
        this.myPeerID = myPeerID;
    }

    public void sendMessage(Message message){
        try {
            this.out.writeObject(message);
            this.out.flush();
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }
    public boolean isAlive(){
        return this.connectedToSocket.isConnected();
    }


}
