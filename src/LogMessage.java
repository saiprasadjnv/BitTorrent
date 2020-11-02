public enum LogMessage {
    // called using these log messages and string array of args
    CLIENT_CONNECT,                 // TCP Connection to peer; args = [neighbour_peer:String]
    SERVER_CONNECT,                 // TCP Connection from peer; args = [neighbour_peer:String]
    CHANGE_PREFERRED_NEIGHBOURS,    // change of preferred neighbours; args = peers:String[]
    CHANGE_UNCHOKED_NEIGHBOUR,      // change of optimistically unchoked neighbour; args = [optimistically_unchoked_neighbourid:String]
    UNCHOKING,                      // unchoking; args = [neighbour_peer:String]
    CHOKING,                        // choking; args = [neighbour_peer:String]
    RECEIVE_HAVE,                   // receiving ‘have’ message; args = [neighbour_peer:String,piece_index:String]
    RECEIVE_INTERESTED,             // receiving ‘interested’ message; args = [neighbour_peer:String]
    RECEIVE_NOT_INTERESTED,         // receiving ‘not interested’ message; args = [neighbour_peer:String]
    PIECE_DOWNLOAD,                 // downloading a piece; args = [neighbour_peer:String,piece_index:String,num_pieces:String]
    DOWNLOAD_COMPLETE               // completion of download; args = null
}
