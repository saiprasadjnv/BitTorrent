import java.util.*;

public class NeighbourHandler {
    MessageHandler messageHandler;
    peerProcess myProcess;
    int numPreferredNeighbours;
    Timer t;

    public NeighbourHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        this.myProcess = messageHandler.myProcess;
        this.numPreferredNeighbours = messageHandler.myProcess.numberOfPreferredNeighbours;
        t = new Timer();
    }

    public void runUnchokeTasks() {
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                setOptimisticNeighbour();
            }
        }, 0, myProcess.optimisticUnchokingInterval * 1000);
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                setPreferredNeighbours();
            }
        }, 0, myProcess.unchokingInterval * 1000);
    }

    private void setOptimisticNeighbour() {

    }

    private void setPreferredNeighbours() {

    }

    public void stopTasks() {
        t.cancel();
    }
}
