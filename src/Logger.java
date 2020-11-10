import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class Logger {
    private String peerId;
    private String logFileName;
    private FileOutputStream logFileStream;
    private OutputStreamWriter out;

    public Logger(String peerId) {
        this.peerId = peerId;
        this.logFileName = "log_peer_" + peerId + ".log";
    }

    public void start() throws FileNotFoundException {
        this.logFileStream = new FileOutputStream(this.logFileName, false);
        this.out = new OutputStreamWriter(this.logFileStream);  // default encoding - UTF-8
    }

    public void stop() throws IOException {
        out.flush();
        logFileStream.close();
    }

    public void writeLog(LogMessage logMessage, String[] args) throws Exception {
        // args is always array of strings; their structure based on log message is mentioned in LogMessage.java
        String text = null;
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        try {
            switch (logMessage) {
                case CLIENT_CONNECT:
                    text = "Peer " + this.peerId + " makes a connection to Peer " + args[0] + ".";
                    break;
                case SERVER_CONNECT:
                    text = "Peer " + this.peerId + " is connected from Peer " + args[0] + ".";
                    break;
                case CHANGE_PREFERRED_NEIGHBOURS:
                    text = "Peer " + this.peerId + " has the preferred neighbors " + String.join(", ", args) + ".";
                    break;
                case CHANGE_UNCHOKED_NEIGHBOUR:
                    text = args[0] + " is the peer ID of the optimistically unchoked neighbor.";
                    break;
                case UNCHOKING:
                    text = "Peer " + this.peerId + " is unchoked by " + args[0] + ".";
                    break;
                case CHOKING:
                    text = "Peer " + this.peerId + " is choked by " + args[0] + ".";
                    break;
                case RECEIVE_HAVE:
                    text = "Peer " + this.peerId + " received the 'have' message from " + args[0] + " for the piece " + args[1] + ".";
                    break;
                case RECEIVE_INTERESTED:
                    text = "Peer " + this.peerId + " received the 'interested' message from " + args[0] + ".";
                    break;
                case RECEIVE_NOT_INTERESTED:
                    text = "Peer " + this.peerId + " received the 'not interested' message from " + args[0] + ".";
                    break;
                case PIECE_DOWNLOAD:
                    text = "Peer " + this.peerId + " has downloaded the piece " + args[1] + " from " + args[0] + ". Now the number of pieces it has is " + args[2] + ".";
                    break;
                case DOWNLOAD_COMPLETE:
                    text = "Peer " + this.peerId + " has downloaded the complete file.";
                    break;
                default:
                    throw new Exception("Invalid Log Message");
            }
            out.write(dateTime + ": " + text + "\n");
        } catch (
                Exception ex) {
            ex.printStackTrace();
        }
    }
}