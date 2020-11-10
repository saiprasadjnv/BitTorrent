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
        }, 0, myProcess.optimisticUnchokingInterval*1000);
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                setPreferredNeighbours();
            }
        }, 0, myProcess.unchokingInterval*1000);
    }

    private void setOptimisticNeighbour() {
        if (!this.messageHandler.interestedPeers.isEmpty()) {
            Vector<String> v = new Vector<>();
            for (String pid : this.messageHandler.interestedPeers) {
                if (!this.messageHandler.unchokeStatus.get(pid))
                    v.add(pid);
            }
            if (v.size() > 0) {
                String s = v.elementAt(new Random().nextInt(v.size()));
                // since we are accessing an atomic variable, we can use its thread safe methods but only function in the project that changes this variable is only this one, so can work it out directly
                if (!s.equals(this.messageHandler.optimizedNeighbour.get())) {
                    if (!this.messageHandler.preferredNeighbours.contains(this.messageHandler.optimizedNeighbour.get())) {
                        this.messageHandler.CreateAndSendChokeMessage(this.messageHandler.peersToTCPConnectionsMapping.get(this.messageHandler.optimizedNeighbour.get()));
                    }
                    this.messageHandler.optimizedNeighbour.set(s);
                    this.messageHandler.CreateAndSendUnchokeMessage(this.messageHandler.peersToTCPConnectionsMapping.get(s));
                }
            } else {
                if (!this.messageHandler.interestedPeers.contains(this.messageHandler.optimizedNeighbour.get())) {
                    // when uninterested, choke message unnecessary
//                    this.messageHandler.CreateAndSendChokeMessage(this.messageHandler.peersToTCPConnectionsMapping.get(this.messageHandler.optimizedNeighbour.get()));
                    this.messageHandler.optimizedNeighbour.set("");
                }
            }
        } else {
            // when uninterested, choke message unnecessary
//            this.messageHandler.CreateAndSendChokeMessage(this.messageHandler.peersToTCPConnectionsMapping.get(this.messageHandler.optimizedNeighbour.get()));
            this.messageHandler.optimizedNeighbour.set("");
        }
    }

    private void setPreferredNeighbours() {
        if (this.myProcess.hasFile) {   // random selection for hasfile = true
            if (!this.messageHandler.interestedPeers.isEmpty()) {
                Vector<String> v = new Vector<>(this.messageHandler.interestedPeers);
                Collections.shuffle(v);
                int endIndex = Math.min(v.size(), this.numPreferredNeighbours);
                this.messageHandler.preferredNeighbours.clear();
                for (String pid : v) {
                    if (v.indexOf(pid) < endIndex) {
                        this.messageHandler.unchokeStatus.put(pid, true);
                        this.messageHandler.preferredNeighbours.add(pid);
                        this.messageHandler.CreateAndSendUnchokeMessage(this.messageHandler.peersToTCPConnectionsMapping.get(pid));
                    } else {
                        this.messageHandler.unchokeStatus.put(pid, false);
                        this.messageHandler.CreateAndSendChokeMessage(this.messageHandler.peersToTCPConnectionsMapping.get(pid));
                    }
                }
            } else {
                for (String pid : this.messageHandler.preferredNeighbours) {
                    this.messageHandler.unchokeStatus.put(pid, false);
                    // when uninterested, choke message unnecessary
//                    this.messageHandler.CreateAndSendChokeMessage(this.messageHandler.peersToTCPConnectionsMapping.get(pid));
                }
            }
        }
//        else {    // no hasfile; based on download rate;
//            if (!this.messageHandler.interestedPeers.isEmpty()) {
//                Vector<String> v = new Vector<>(this.messageHandler.interestedPeers);
//                for (String pid : v) {
//                    TCPConnectionInfo tci = this.messageHandler.peersToTCPConnectionsMapping.get(pid);

//                }
//            }
//    }
    }

    public void stopTasks() {
        t.cancel();
    }
}
