public class Message {
    public int messageLength;
    public byte messageType;
    public int pieceIndex;
    public boolean[] bitField;
    public TCPConnectionInfo messageOrigin;

    Message(byte messageType, int messageLength){
        this.messageType = messageType;
        this.messageLength = messageLength;
    }
    /***
     * Constructor for "request" and "have" messages
     */
    Message(byte messageType, int messageLength, int pieceIndex){
        this.messageType = messageType;
        this.messageLength = messageLength;
        this.pieceIndex = pieceIndex;
    }

    /***
     * Constructor for "bitfield" message
     */
    Message(byte messageType, int messageLength, boolean[] bitField){
        this.messageType = messageType;
        this.messageLength = messageLength;
        this.bitField = bitField;
    }

    /**
     * Constructor for "piece" message.
     * */
//    Message(int messageType, int messageLength, int pieceIndex, FilePart filepart){
//
//    }


    /***
     * Default constructor for custom messages. Message type '100' represents custom message'
     * This is used to notify the peerProcess that a new Handshake message is received.
     * */
    Message(){
        this.messageType = 100;
    }

}
