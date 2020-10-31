public class Message {
    public byte messageLength;
    public short messageType;
    public int pieceIndex;
    public boolean[] bitField;
    Message(short messageType, byte messageLength){
        this.messageType = messageType;
        this.messageLength = messageLength;
    }

    Message(short messageType, byte messageLength, int pieceIndex){
        this.messageType = messageType;
        this.messageLength = messageLength;
        this.pieceIndex = pieceIndex;
    }

    Message(short messageType, byte messageLength, boolean[] bitField){
        this.messageType = messageType;
        this.messageLength = messageLength;
        this.bitField = bitField;
    }
}
