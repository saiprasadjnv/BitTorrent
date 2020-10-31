import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Runnable;
import java.util.Set;
import java.util.Vector;

public class ListenerThread implements Runnable {
    private TCPConnectionInfo monitorConnection;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    public ListenerThread(TCPConnectionInfo monitorConnection){
        this.monitorConnection = monitorConnection;
        this.outputStream = monitorConnection.out;
        this.inputStream = monitorConnection.in;
    }
    private static boolean verifyHandshake(HandshakeMessage receivedHandshake){
        if(receivedHandshake.handshakeHeader.equals("P2PFILESHARINGPROJ")==false){
            return false;
        }
        return true;
    }
    public void run(){
        System.out.println("Running Listener thread");
        HandshakeMessage myHandshakeMessage = new HandshakeMessage(monitorConnection.myPeerID);
        try {
            System.out.println("Sending handshake message");
            System.out.println("outStream in thread: "+ outputStream.hashCode());
            System.out.println("inStream in thread: " + inputStream.hashCode());
            this.outputStream.writeObject(myHandshakeMessage);
            this.outputStream.flush();
            HandshakeMessage receivedHandshake= (HandshakeMessage) this.inputStream.readObject();
            this.monitorConnection.associatedPeerId = receivedHandshake.peerID;
            boolean isValidHandshake = verifyHandshake(receivedHandshake);
            while (true){
                Message newMessage = (Message) inputStream.readObject();
            }
        }catch (IOException ex){

        }catch (ClassNotFoundException ex){

        }

    }
}
