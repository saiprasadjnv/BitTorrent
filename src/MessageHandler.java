import jdk.jshell.execution.Util;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class MessageHandler implements Runnable {
    peerProcess myProcess;
    public ConcurrentLinkedQueue<Message> messageQueue;
    public ConcurrentHashMap<String, TCPConnectionInfo> peersToTCPConnectionsMapping;

    MessageHandler(peerProcess myProcess) {
        this.myProcess = myProcess;
        this.peersToTCPConnectionsMapping = myProcess.peersToTCPConnectionsMapping;
        this.messageQueue = myProcess.messageQueue;
    }

    /**
     * Each of the below CreateAndSend<MessageType>Message() function creates a message object with the required fields.
     * Then it sends the message to corresponding peer through the TCPConnection.
     */

    private void CreateAndSendBitFieldMessage(TCPConnectionInfo associatedTCPConnection) {
        try {
            if (!associatedTCPConnection.isAlive()) {
                System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
            } else {
                Message bitFieldMessage = new Message((byte) 5, 1 + myProcess.myBitField.length, myProcess.myBitField);
                associatedTCPConnection.sendMessage(bitFieldMessage);
            }
        }catch (NullPointerException|IOException ex){
            ex.printStackTrace();
        }
    }

    private void CreateAndSendHaveMessage(TCPConnectionInfo associatedTCPConnection, int pieceID) {
        // Have message
        try {
            if (!associatedTCPConnection.isAlive()) {
                System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
            } else {
                Message haveMessage = new Message((byte) 4, 5, pieceID);
                associatedTCPConnection.sendMessage(haveMessage);
            }
        }catch (NullPointerException|IOException ex){
            ex.printStackTrace();
        }
    }

    private void CreateAndSendRequestMessage(TCPConnectionInfo associatedTCPConnection, int pieceID) {
        try {
            if (!associatedTCPConnection.isAlive()) {
                System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
            } else {
                Message requestMessage = new Message((byte) 6, 5, pieceID);
                associatedTCPConnection.sendMessage(requestMessage);
            }
        }catch (NullPointerException|IOException ex){
            ex.printStackTrace();
        }
    }

    private void CreateAndSendInterestedMessage(TCPConnectionInfo associatedTCPConnection) {
        try {
            if (!associatedTCPConnection.isAlive()) {
                System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
            } else {
                Message interestedMessage = new Message((byte) 2, 1);
                associatedTCPConnection.sendMessage(interestedMessage);
            }
        }catch (NullPointerException|IOException ex){
            ex.printStackTrace();
        }
    }

    private void CreateAndSendNotInterestedMessage(TCPConnectionInfo associatedTCPConnection) {
        try {
            if (!associatedTCPConnection.isAlive()) {
                System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
            } else {
                Message notInterestedMessage = new Message((byte) 3, 1);
                associatedTCPConnection.sendMessage(notInterestedMessage);
            }
        }catch (NullPointerException|IOException ex){
            ex.printStackTrace();
        }
    }

    protected void CreateAndSendChokeMessage(TCPConnectionInfo associatedTCPConnection) {
        try {
            if (!associatedTCPConnection.isAlive()) {
                System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
            } else {
                Message chokeMessage = new Message((byte) 0, 1);
                associatedTCPConnection.sendMessage(chokeMessage);
            }
        }catch (NullPointerException|IOException ex){
            ex.printStackTrace();
        }
    }

    protected void CreateAndSendUnchokeMessage(TCPConnectionInfo associatedTCPConnection) {
        try {
            if (!associatedTCPConnection.isAlive()) {
                System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
            } else {
                Message unChokeMessage = new Message((byte) 1, 1);
                associatedTCPConnection.sendMessage(unChokeMessage);
            }
        } catch (NullPointerException|IOException ex){
            ex.printStackTrace();
        }
    }

    private void CreateAndSendPieceMessage(TCPConnectionInfo associatedTCPConnection, int pieceIndex, byte[] piece) {
        try {
            if (!associatedTCPConnection.isAlive()) {
                System.out.println("TCP Connection to peer " + associatedTCPConnection.myPeerID + " is not alive!!");
            } else {
                Message pieceMessage = new Message((byte) 7, 5 + myProcess.pieceSize, pieceIndex, piece);
                associatedTCPConnection.sendMessage(pieceMessage);
            }
        }catch (NullPointerException| IOException ex){
            ex.printStackTrace();
        }
    }

    public void run() {
        while (!Thread.interrupted()) {
//            System.out.print("In MessageHandler");
            if (!messageQueue.isEmpty()) {
                Message newMessage = messageQueue.remove();
//                System.out.println("Received message type: " + newMessage.messageType + "; From: " + newMessage.messageOrigin.associatedPeerId);
                String peerId = newMessage.messageOrigin.associatedPeerId;
                switch (newMessage.messageType) {
                    case 0:
                        //Handle Choke message
                        myProcess.canRequestStatus.put(peerId, false);
                        break;
                    case 1:
                        //Handle Unchoke message
                        myProcess.canRequestStatus.put(peerId, true);
                        boolean[] bitField = myProcess.bitFieldsOfPeers.get(peerId);
                        for(int pieceIndex=1; pieceIndex<=myProcess.numberOfPieces && myProcess.canRequestStatus.get(peerId); pieceIndex++) {
                            if (!myProcess.myBitField[pieceIndex-1] && bitField[pieceIndex-1] && !myProcess.requestedPieces.contains(pieceIndex)){
                                CreateAndSendRequestMessage(newMessage.messageOrigin, pieceIndex);
                                myProcess.requestedPieces.add(pieceIndex);
                            }
                        }
                        break;
                    case 2:
                        //Handle Interested message
                        if (myProcess.peerInfoMap.containsKey(peerId)) {
                            if (!myProcess.interestedPeers.contains(peerId))
                                myProcess.interestedPeers.add(peerId);
                        }
                        break;
                    case 3:
                        //Handle Not interested message
                        myProcess.interestedPeers.remove(peerId);
                        break;
                    case 4:
                        //Handle Have message
                        //Update the associatedPeerBitfield
                        int pieceIndex = newMessage.pieceIndex;
                        boolean[] currentBitField = myProcess.bitFieldsOfPeers.get(peerId);
//                        System.out.println(currentBitField.length + ": " + peerId);
                        currentBitField[pieceIndex-1] = true;
                        myProcess.bitFieldsOfPeers.put(peerId, currentBitField);
                        //Check if you are interested in this piece.


                        if (!myProcess.myBitField[pieceIndex-1]) {
                            CreateAndSendInterestedMessage(newMessage.messageOrigin);
                        } else {
                            CreateAndSendNotInterestedMessage(newMessage.messageOrigin);
                        }
                        break;
                    case 5:
                        //Handle bitfield message
                        String connectedPeer = newMessage.messageOrigin.associatedPeerId;
//                        System.out.println("Received bit-field message from :" + connectedPeer);
//                        Utility.printBooleanArray(newMessage.bitField);
                        boolean interested = false;
                        for (int i = 0; i < myProcess.numberOfPieces; i++) {
                            if (newMessage.bitField[i] && !myProcess.myBitField[i]) {
                                interested = true;
                                break;
                            }
                        }
                        if (interested) {
                            CreateAndSendInterestedMessage(newMessage.messageOrigin);
                        } else {
                            CreateAndSendNotInterestedMessage(newMessage.messageOrigin);
                        }
                        //TODO Just for testing. Remove Later..!!
                        CreateAndSendUnchokeMessage(newMessage.messageOrigin);
                        //........................................
                        myProcess.bitFieldsOfPeers.put(connectedPeer, newMessage.bitField);
                        break;
                    case 6:
                        //Handle request message
                        if(myProcess.unchokeStatus.get(peerId)){
                            int requestedPiece = newMessage.pieceIndex;
                            int pieceSize = myProcess.pieceSize;
                            if(requestedPiece== myProcess.numberOfPieces){
                                pieceIndex = myProcess.lastPieceSize;
                            }
                            byte[] piece = myProcess.myFileObject.readPiece(requestedPiece, pieceSize);
                            CreateAndSendPieceMessage(newMessage.messageOrigin,requestedPiece, piece);
                        }
                        break;
                    case 7:
                        //Handle Piece message
                        pieceIndex = newMessage.pieceIndex;
                        int offset = (pieceIndex - 1) * myProcess.pieceSize;
                        int piecesize = myProcess.pieceSize;
                        if(pieceIndex== myProcess.numberOfPieces){
                            piecesize = myProcess.lastPieceSize;
                        }
                        boolean pieceDownloaded = myProcess.myFileObject.writePiece(pieceIndex, newMessage.piece, piecesize);
                        if (pieceDownloaded) {
                            //update bitField
                            myProcess.myBitField[pieceIndex-1] = true;
                            for (TCPConnectionInfo conn : myProcess.peersToTCPConnectionsMapping.values()) {
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
        System.out.println("Exiting from Message Handler");
    }
}
