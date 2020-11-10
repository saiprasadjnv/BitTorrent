import java.io.Serializable;

public class Message implements Serializable {
    protected int messageLength;
    protected byte messageType;
    protected int pieceIndex;
    protected boolean[] bitField;
    protected byte[] piece;
    public TCPConnectionInfo messageOrigin;

    Message(byte messageType, int messageLength) {
        this.messageType = messageType;
        this.messageLength = messageLength;
    }

    /***
     * Constructor for "request" and "have" messages
     */
    Message(byte messageType, int messageLength, int pieceIndex) {
        this.messageType = messageType;
        this.messageLength = messageLength;
        this.pieceIndex = pieceIndex;
    }

    /***
     * Constructor for "bitfield" message
     */
    Message(byte messageType, int messageLength, boolean[] bitField) {
        this.messageType = messageType;
        this.messageLength = messageLength;
        this.bitField = bitField;
    }

    /**
     * Constructor for "piece" message.
     */
    Message(byte messageType, int messageLength, int pieceIndex, byte[] piece) {
        this.messageType = messageType;
        this.messageLength = messageLength;
        this.pieceIndex = pieceIndex;
        this.piece = piece;
    }

    /***
     * Default constructor for custom messages. Message type '100' represents custom message'
     * This is used to notify the peerProcess that a new Handshake message is received.
     * */
    Message() {
        this.messageType = 100;
    }

}
