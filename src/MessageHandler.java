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

    private void CreateAndSendBitFieldMessage(TCPConnectionInfo associatedTCPConnection){
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection closed to peer: " + associatedTCPConnection.myPeerID);
        }
        Message bitFieldMessage = new Message((byte) 5, 1+ myProcess.myBitField.length, myProcess.myBitField);
        associatedTCPConnection.sendMessage(bitFieldMessage);
    }

    private void CreateAndSendHaveMessage(TCPConnectionInfo associatedTCPConnection, int pieceID){
        // Have message
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection closed to peer: " + associatedTCPConnection.myPeerID);
        }
        Message haveMessage = new Message((byte)4, 5, pieceID);
        associatedTCPConnection.sendMessage(haveMessage);
    }

    private void CreateAndSendRequestMessage(TCPConnectionInfo associatedTCPConnection, int pieceID){
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection closed to peer: " + associatedTCPConnection.myPeerID);
        }
        Message requestMessage = new Message((byte)6, 5, pieceID);
        associatedTCPConnection.sendMessage(requestMessage);
    }

    private void CreateAndSendInterestedMessage(TCPConnectionInfo associatedTCPConnection){
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection closed to peer: " + associatedTCPConnection.myPeerID);
        }
        Message interestedMessage = new Message((byte)2, 1);
        associatedTCPConnection.sendMessage(interestedMessage);
    }

    private void CreateAndSendNotInterestedMessage(TCPConnectionInfo associatedTCPConnection){
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection closed to peer: " + associatedTCPConnection.myPeerID);
        }
        Message notInterestedMessage = new Message((byte)3, 1);
        associatedTCPConnection.sendMessage(notInterestedMessage);
    }

    private void CreateAndSendChokeMessage(TCPConnectionInfo associatedTCPConnection){
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection closed to peer: " + associatedTCPConnection.myPeerID);
        }
        Message chokeMessage = new Message((byte)0, 1);
        associatedTCPConnection.sendMessage(chokeMessage);
    }

    private void CreateAndSendUnchokeMessage(TCPConnectionInfo associatedTCPConnection){
        if(!associatedTCPConnection.isAlive()){
            System.out.println("TCP Connection closed to peer: " + associatedTCPConnection.myPeerID);
        }
        Message unChokeMessage = new Message((byte)1, 1);
        associatedTCPConnection.sendMessage(unChokeMessage);
    }

//TODO: Need to implement the SendPieceMessage
//    private void CreateAndSendPieceMessage(TCPConnectionInfo associatedTCPConnection){
//
//    }

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
                        break;
                    case 5:
                        //Handle bitfield message
                        String connectedPeer  = newMessage.messageOrigin.associatedPeerId;
                        System.out.println("Received bit-field message from :" + connectedPeer);
                        //Utility.printBooleanArray(newMessage.bitField);
                        myProcess.bitFieldsOfPeers.put(connectedPeer, newMessage.bitField);
                        break;
                    case 6:
                        //Handle request message
                        break;
                    case 7:
                        //Handle Piece message
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
