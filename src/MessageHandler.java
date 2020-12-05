import jdk.jshell.execution.Util;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;


public class MessageHandler implements Runnable {
    public peerProcess myProcess;
    public ConcurrentLinkedQueue<Message> messageQueue;
    public ConcurrentHashMap<String, TCPConnectionInfo> peersToTCPConnectionsMapping;
    public HashSet<Integer> requestedPieces;
    public HashSet<Integer> receivedPieces;

    MessageHandler(peerProcess myProcess) {
        this.myProcess = myProcess;
        this.peersToTCPConnectionsMapping = myProcess.peersToTCPConnectionsMapping;
        this.messageQueue = myProcess.messageQueue;
        this.requestedPieces = new HashSet<>();
        this.receivedPieces = new HashSet<>();
    }

    /**
     * Each of the below CreateAndSend<MessageType>Message() function creates a message object with the required fields.
     * Then it sends the message to corresponding peer through the TCPConnection.
     */

    private void CreateAndSendBitFieldMessage(TCPConnectionInfo associatedTCPConnection) {
        try {
            if (associatedTCPConnection.isAlive()) {
                Message bitFieldMessage = new Message((byte) 5, 1 + myProcess.myBitField.length, myProcess.myBitField);
                associatedTCPConnection.sendMessage(bitFieldMessage);
            }
        } catch (NullPointerException | IOException ex) {
            ex.printStackTrace();
        }
    }

    private void CreateAndSendHaveMessage(TCPConnectionInfo associatedTCPConnection, int pieceID) {
        // Have message
        try {
            if (associatedTCPConnection.isAlive()) {
                Message haveMessage = new Message((byte) 4, 5, pieceID);
                associatedTCPConnection.sendMessage(haveMessage);
            }
        } catch (NullPointerException | IOException ex) {
            ex.printStackTrace();
        }
    }

    private void CreateAndSendRequestMessage(TCPConnectionInfo associatedTCPConnection, int pieceID) {
        try {
            if (associatedTCPConnection.isAlive()) {
                Message requestMessage = new Message((byte) 6, 5, pieceID);
                associatedTCPConnection.sendMessage(requestMessage);
            }
        } catch (NullPointerException | IOException ex) {
            ex.printStackTrace();
        }
    }

    private void CreateAndSendInterestedMessage(TCPConnectionInfo associatedTCPConnection) {
        try {
            if (associatedTCPConnection.isAlive()) {
                Message interestedMessage = new Message((byte) 2, 1);
                associatedTCPConnection.sendMessage(interestedMessage);
            }
        } catch (NullPointerException | IOException ex) {
            ex.printStackTrace();
        }
    }

    private void CreateAndSendNotInterestedMessage(TCPConnectionInfo associatedTCPConnection) {
        try {
            if (associatedTCPConnection.isAlive()) {
                Message notInterestedMessage = new Message((byte) 3, 1);
                associatedTCPConnection.sendMessage(notInterestedMessage);
            }
        } catch (NullPointerException | IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void CreateAndSendChokeMessage(TCPConnectionInfo associatedTCPConnection) {
        try {
            if (associatedTCPConnection.isAlive()) {
                Message chokeMessage = new Message((byte) 0, 1);
                associatedTCPConnection.sendMessage(chokeMessage);
            }
        } catch (NullPointerException | IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void CreateAndSendUnchokeMessage(TCPConnectionInfo associatedTCPConnection) {
        try {
            if (associatedTCPConnection.isAlive()) {
                Message unChokeMessage = new Message((byte) 1, 1);
                associatedTCPConnection.sendMessage(unChokeMessage);
            }
        } catch (NullPointerException | IOException ex) {
            ex.printStackTrace();
        }
    }

    private void CreateAndSendPieceMessage(TCPConnectionInfo associatedTCPConnection, int pieceIndex, byte[] piece) {
        try {
            if (associatedTCPConnection.isAlive()) {
                Message pieceMessage = new Message((byte) 7, 5 + myProcess.pieceSize, pieceIndex, piece);
                associatedTCPConnection.sendMessage(pieceMessage);
            }
        } catch (NullPointerException | IOException ex) {
            ex.printStackTrace();
        }
    }

    private int getARandomInterestingPiece(String associatedPeer) {
        boolean[] bitField = myProcess.bitFieldsOfPeers.get(associatedPeer);
        ArrayList<Integer> interestingPieces = new ArrayList<>();
        for (int pieceIndex = 1; pieceIndex <= myProcess.numberOfPieces; pieceIndex++) {
            if (!myProcess.myBitField[pieceIndex - 1] && bitField[pieceIndex - 1] && !myProcess.requestedPieces.contains(pieceIndex)) {
                interestingPieces.add(pieceIndex);
            }
        }
        if (interestingPieces.size() == 0) {
            return -1;
        }
        Random random = new Random();
        int requestPiece;
        do {
            int randomIndex = random.nextInt(interestingPieces.size());
            requestPiece = interestingPieces.get(randomIndex);
        } while (myProcess.requestedPieces.contains(requestPiece) || myProcess.downloadedPieces.contains(requestPiece));
        return requestPiece;
    }

    public void run() {
        while (!Thread.interrupted()) {
//            //System.out.print("In MessageHandler");
            if (!messageQueue.isEmpty()) {
                Message newMessage = messageQueue.remove();
//                //System.out.println("Received message type: " + newMessage.messageType + "; From: " + newMessage.messageOrigin.associatedPeerId);
                String peerId = newMessage.messageOrigin.associatedPeerId;
                switch (newMessage.messageType) {
                    case 0:
                        //Handle Choke message
                        peerProcess.logger.writeLog(LogMessage.CHOKING, new String[]{peerId});
                        myProcess.canRequestStatus.put(peerId, false);
                        break;
                    case 1:
                        //Handle Unchoke message
                        peerProcess.logger.writeLog(LogMessage.UNCHOKING, new String[]{peerId});
                        if (!myProcess.canRequestStatus.get(peerId)) {
                            myProcess.canRequestStatus.put(peerId, true);
                            int requestPiece = getARandomInterestingPiece(peerId);
                            if (requestPiece > 0 && !myProcess.requestedPieces.contains(requestPiece)) {
                                myProcess.requestedPieces.add(requestPiece);
                                CreateAndSendRequestMessage(newMessage.messageOrigin, requestPiece);
                            }
                        }
                        break;
                    case 2:
                        //Handle Interested message
                        peerProcess.logger.writeLog(LogMessage.RECEIVE_INTERESTED,new String[]{peerId});
                        if (myProcess.peerInfoMap.containsKey(peerId)) {
                            if (!myProcess.interestedPeers.contains(peerId))
                                myProcess.interestedPeers.add(peerId);
                        }
                        break;
                    case 3:
                        //Handle Not interested message
                        peerProcess.logger.writeLog(LogMessage.RECEIVE_NOT_INTERESTED,new String[]{peerId});
                        myProcess.interestedPeers.remove(peerId);
                        break;
                    case 4:
                        //Handle Have message
                        //Update the associatedPeerBitfield
                        int pieceIndex = newMessage.pieceIndex;
                        peerProcess.logger.writeLog(LogMessage.RECEIVE_HAVE, new String[]{peerId, Integer.toString(pieceIndex)});
                        boolean[] currentBitField = myProcess.bitFieldsOfPeers.get(peerId);
//                        //System.out.println(currentBitField.length + ": " + peerId);
                        currentBitField[pieceIndex - 1] = true;
                        myProcess.bitFieldsOfPeers.put(peerId, currentBitField);
                        //Check if you are interested in this piece.

                        if (!myProcess.myBitField[pieceIndex - 1]) {
                            CreateAndSendInterestedMessage(newMessage.messageOrigin);
                        } else {
                            CreateAndSendNotInterestedMessage(newMessage.messageOrigin);
                        }
                        break;
                    case 5:
                        //Handle bitfield message
                        String connectedPeer = newMessage.messageOrigin.associatedPeerId;
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
                        myProcess.bitFieldsOfPeers.put(connectedPeer, newMessage.bitField);
                        break;
                    case 6:
                        //Handle request message
                        int requestedPiece = newMessage.pieceIndex;
                        if (myProcess.unchokeStatus.get(peerId) && requestedPiece > 0 && requestedPiece <= myProcess.numberOfPieces) {
                            int pieceSize = myProcess.pieceSize;
                            if (requestedPiece == myProcess.numberOfPieces) {
                                pieceSize = myProcess.lastPieceSize;
                            }
                            byte[] piece = myProcess.myFileObject.readPiece(requestedPiece, pieceSize);
                            CreateAndSendPieceMessage(newMessage.messageOrigin, requestedPiece, piece);
                        }
                        break;
                    case 7:
                        //Handle Piece message
                        pieceIndex = newMessage.pieceIndex;
                        if (!myProcess.downloadedPieces.contains(pieceIndex)) {
                            //System.out.println("Received piece " + pieceIndex + " from "+ peerId);
                            int offset = (pieceIndex - 1) * myProcess.pieceSize;
                            int pieceSize = myProcess.pieceSize;
                            if (pieceIndex == myProcess.numberOfPieces) {
                                pieceSize = myProcess.lastPieceSize;
                            }
                            //System.out.println("Writing piece " + pieceIndex + " to file!! from " + peerId);
                            boolean pieceDownloaded = myProcess.myFileObject.writePiece(pieceIndex, newMessage.piece, pieceSize);
                            //System.out.println("Downloaded piece successfully!! : " + peerId + ":::" + pieceIndex);
                            if (pieceDownloaded) {
                                //update bitField
                                peerProcess.logger.writeLog(LogMessage.PIECE_DOWNLOAD,new String[]{peerId,Integer.toString(pieceIndex),Integer.toString(myProcess.downloadedPieces.size())});
                                myProcess.downloadedPieces.add(pieceIndex);
                                myProcess.downloadRate.put(peerId, myProcess.downloadRate.get(peerId) + 1);
                                System.out.println("Received " + myProcess.downloadedPieces.size() + " pieces out of " + myProcess.numberOfPieces + " pieces");
                                System.out.println("Requested " + myProcess.requestedPieces.size() + " pieces out of " + myProcess.numberOfPieces + " pieces");
                                myProcess.myBitField[pieceIndex - 1] = true;
                                for (TCPConnectionInfo conn : myProcess.peersToTCPConnectionsMapping.values()) {
                                    CreateAndSendHaveMessage(conn, pieceIndex);
                                }
                                if (myProcess.canRequestStatus.get(peerId)) {
                                    int requestPiece = getARandomInterestingPiece(peerId);
                                    if (requestPiece > 0) {
                                        myProcess.requestedPieces.add(requestPiece);
                                        CreateAndSendRequestMessage(newMessage.messageOrigin, requestPiece);
                                    }
                                }
                            }
                        }
                        break;
                    case 100:
                        //Handle Handshake message
                        if(peerId.compareTo(myProcess.peerId)>0) {
                            peerProcess.logger.writeLog(LogMessage.CLIENT_CONNECT, new String[]{peerId});
                        }else{
                            peerProcess.logger.writeLog(LogMessage.SERVER_CONNECT, new String[]{peerId});
                        }
                        CreateAndSendBitFieldMessage(newMessage.messageOrigin);
                        break;
                    default:
                        //System.out.println("Invalid Message type received");
                }
            }
        }
        //System.out.println("Exiting from Message Handler");
    }
}
