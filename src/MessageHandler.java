import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageHandler implements Runnable{
    peerProcess myProcess;
    public ConcurrentLinkedQueue<Message> messageQueue;
    public ConcurrentHashMap<String, TCPConnectionInfo> peersToTCPConnectionsMapping;

    MessageHandler(peerProcess myProcess){
        this.myProcess = myProcess;
        this.peersToTCPConnectionsMapping = myProcess.peersToTCPConnectionsMapping;
        this.messageQueue = myProcess.messageQueue;
    }
    /**
     * Each of the below CreateAndSend<MessageType>Message() function creates a message object with the required fields.
     * Then it sends the message to corresponding peer through the TCPConnection.
     * */

    private void CreateAndSendBitFieldMessage(TCPConnectionInfo associatedTCPConnection){
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
        }else {
            Message bitFieldMessage = new Message((byte) 5, 1 + myProcess.myBitField.length, myProcess.myBitField);
            associatedTCPConnection.sendMessage(bitFieldMessage);
        }
    }

    private void CreateAndSendHaveMessage(TCPConnectionInfo associatedTCPConnection, int pieceID){
        // Have message
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
        }else {
            Message haveMessage = new Message((byte) 4, 5, pieceID);
            associatedTCPConnection.sendMessage(haveMessage);
        }
    }

    private void CreateAndSendRequestMessage(TCPConnectionInfo associatedTCPConnection, int pieceID){
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
        }else {
            Message requestMessage = new Message((byte) 6, 5, pieceID);
            associatedTCPConnection.sendMessage(requestMessage);
        }
    }

    private void CreateAndSendInterestedMessage(TCPConnectionInfo associatedTCPConnection){
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
        }else {
            Message interestedMessage = new Message((byte) 2, 1);
            associatedTCPConnection.sendMessage(interestedMessage);
        }
    }

    private void CreateAndSendNotInterestedMessage(TCPConnectionInfo associatedTCPConnection){
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
        }else {
            Message notInterestedMessage = new Message((byte) 3, 1);
            associatedTCPConnection.sendMessage(notInterestedMessage);
        }
    }

    private void CreateAndSendChokeMessage(TCPConnectionInfo associatedTCPConnection){
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
        }else {
            Message chokeMessage = new Message((byte) 0, 1);
            associatedTCPConnection.sendMessage(chokeMessage);
        }
    }

    private void CreateAndSendUnchokeMessage(TCPConnectionInfo associatedTCPConnection){
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
        }else {
            Message unChokeMessage = new Message((byte) 1, 1);
            associatedTCPConnection.sendMessage(unChokeMessage);
        }
    }

    private void CreateAndSendPieceMessage(TCPConnectionInfo associatedTCPConnection, int pieceIndex, byte[] piece){
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
        }else{
            Message pieceMessage = new Message((byte)7, 5 + myProcess.pieceSize, pieceIndex, piece);
            associatedTCPConnection.sendMessage(pieceMessage);
        }
    }

    public void run(){
        while(true){
            if(!messageQueue.isEmpty()) {
                Message newMessage = messageQueue.remove();
                System.out.println("Received message type: " + newMessage.messageType + "; From: " + newMessage.messageOrigin.associatedPeerId);
                switch (newMessage.messageType) {
                    case 0:
                        //Handle Choke message
                        break;
                    case 1:
                        //Handle Unchoke message
                        break;
                    case 2:
                        //Handle Interested message
                        break;
                    case 3:
                        //Handle Not interested message
                        break;
                    case 4:
                        //Handle Have message
                        //Update the associatedPeerBitfield
                        int pieceIndex = newMessage.pieceIndex;
                        String peerID = newMessage.messageOrigin.associatedPeerId;
                        boolean[] currentBitField = myProcess.bitFieldsOfPeers.get(peerID);
                        currentBitField[pieceIndex] = true;
                        myProcess.bitFieldsOfPeers.put(peerID,currentBitField);
                        //Check if you are interested in this piece.
                        if(!myProcess.myBitField[pieceIndex]){
                            CreateAndSendInterestedMessage(newMessage.messageOrigin);
                        }else{
                            CreateAndSendNotInterestedMessage(newMessage.messageOrigin);
                        }
                        break;
                    case 5:
                        //Handle bitfield message
                        String connectedPeer  = newMessage.messageOrigin.associatedPeerId;
                        System.out.println("Received bit-field message from :" + connectedPeer);
                        Utility.printBooleanArray(newMessage.bitField);
                        boolean interested = false;
                        for(int i=0; i<myProcess.numberOfPieces; i++){
                            if(newMessage.bitField[i] && !myProcess.myBitField[i]){
                                interested = true;
                                break;
                            }
                        }
                        if(interested){
                            CreateAndSendInterestedMessage(newMessage.messageOrigin);
                        }else{
                            CreateAndSendNotInterestedMessage(newMessage.messageOrigin);
                        }
                        myProcess.bitFieldsOfPeers.put(connectedPeer, newMessage.bitField);
                        break;
                    case 6:
                        //Handle request message
                        break;
                    case 7:
                        //Handle Piece message
                        byte[] piece = newMessage.piece;
                        pieceIndex = newMessage.pieceIndex;
                        int offset = (pieceIndex-1)*myProcess.pieceSize;
                        boolean pieceDownloaded = myProcess.myFileObject.writePiece(pieceIndex, piece);
                        if(pieceDownloaded){
                            //update bitField
                            myProcess.myBitField[pieceIndex] = true;
                            for(TCPConnectionInfo conn: myProcess.peersToTCPConnectionsMapping.values()){
                                CreateAndSendHaveMessage(conn, pieceIndex);
                            }
                        }
                        break;
                    case 100:
                        //Handle Handshake message
                        CreateAndSendBitFieldMessage(newMessage.messageOrigin);
                        break;
                    default:
                        System.out.println("Invalid Message type received");
                }
            }

        }
    }
}
