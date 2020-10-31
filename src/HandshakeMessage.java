import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Vector;

public class HandshakeMessage implements Serializable {
    public String handshakeHeader;
    public boolean[] zeroBits;
    public String peerID;
    HandshakeMessage(String peerID){
        this.handshakeHeader = "P2PFILESHARINGPROJ";
        this.peerID = peerID;
        this.zeroBits = new boolean[80];
        Arrays.fill(zeroBits,Boolean.FALSE);
    }
}
