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
        if (!this.myProcess.interestedPeers.isEmpty()) {
            Vector<String> v = new Vector<>(this.myProcess.interestedPeers);
            String s = v.elementAt(new Random().nextInt(v.size()));
            if (!s.equals(this.myProcess.optimizedNeighbour.get())) {
                if (!this.myProcess.preferredNeighbours.contains(this.myProcess.optimizedNeighbour.get())) {
                    this.messageHandler.CreateAndSendChokeMessage(this.myProcess.peersToTCPConnectionsMapping.get(this.myProcess.optimizedNeighbour.get()));
                }
                this.myProcess.optimizedNeighbour.set(s);
                peerProcess.logger.writeLog(LogMessage.CHANGE_OPTIMISTIC_NEIGHBOUR,new String[]{s});
                this.messageHandler.CreateAndSendUnchokeMessage(this.myProcess.peersToTCPConnectionsMapping.get(s));
            }
        } else {
            // when uninterested, choke message unnecessary
//            this.messageHandler.CreateAndSendChokeMessage(this.messageHandler.peersToTCPConnectionsMapping.get(this.messageHandler.optimizedNeighbour.get()));
            this.myProcess.optimizedNeighbour.set("");
        }
    }

    private void setPreferredNeighbours() {
        if (!this.myProcess.interestedPeers.isEmpty()) {
            Vector<String> v = new Vector<>(this.myProcess.interestedPeers);
            Vector<String> sv = new Vector<>();
            if (this.myProcess.hasFile)
                Collections.shuffle(v);
            else {
                Comparator<String> sortbyDownloadRate = (String s1, String s2) -> this.myProcess.downloadRate.get(s1).compareTo(this.myProcess.downloadRate.get(s2));
                Collections.sort(v, sortbyDownloadRate);
            }
            int endIndex = Math.min(v.size(), this.numPreferredNeighbours);

            this.myProcess.preferredNeighbours.clear();
            v.forEach((String pid) -> {
                if (v.indexOf(pid) < endIndex) {
                    this.myProcess.preferredNeighbours.add(pid);
                    sv.add(pid);
                    this.myProcess.unchokeStatus.put(pid, true);
                    this.messageHandler.CreateAndSendUnchokeMessage(this.myProcess.peersToTCPConnectionsMapping.get(pid));
                    if (!this.myProcess.hasFile){
                        this.myProcess.downloadRate.put(pid,0);
                    }
                } else {
                    this.myProcess.unchokeStatus.put(pid, false);
                    if (!pid.equals(this.myProcess.optimizedNeighbour.get()))
                        this.messageHandler.CreateAndSendChokeMessage(this.myProcess.peersToTCPConnectionsMapping.get(pid));
                }
            });
            if (sv.size()>0) {
                String[] sarr = sv.toArray(new String[sv.size()]);
                peerProcess.logger.writeLog(LogMessage.CHANGE_PREFERRED_NEIGHBOURS, sarr);
            }
        } else {
            this.myProcess.preferredNeighbours.forEach((String pid) -> {
                this.myProcess.unchokeStatus.put(pid, false);
            });
            this.myProcess.preferredNeighbours.clear();
        }
    }

    public void stopTasks() {
        t.cancel();
    }
}