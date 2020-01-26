package registry.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {

  // Constants

  private static final String ELECTION_NAMESPACE = "/election";

  // Variables

  private final ZooKeeper mZooKeeper;
  private final OnElectionCallback mCallback;
  private String mCurrentZnodeName;

  // Constructors

  public LeaderElection(ZooKeeper zooKeeper, OnElectionCallback callback) {
    mZooKeeper = zooKeeper;
    mCallback = callback;
  }

  // Overrides

  @Override
  public void process(WatchedEvent watchedEvent) {
    switch (watchedEvent.getType()) {
      case NodeDeleted:
        try {
          reElectLeader();
        } catch (KeeperException | InterruptedException e) {
          e.printStackTrace();
        }
    }
  }

  // Public

  public void volunteerForLeadership() throws KeeperException, InterruptedException {
    final String znodePrefix = ELECTION_NAMESPACE + "/c_";
    final String znoodeFullPath = mZooKeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    mCurrentZnodeName = znoodeFullPath.replace(ELECTION_NAMESPACE + "/", "");

    System.out.println("Znode name: " + znoodeFullPath);
  }

  public void reElectLeader() throws KeeperException, InterruptedException {
    Stat predecessorStat = null;
    String predecessorZnodeName = "";

    while (predecessorStat == null) {
      final List<String> children = mZooKeeper.getChildren(ELECTION_NAMESPACE, false);
      Collections.sort(children);
      final String smallestChild = children.get(0);

      if (smallestChild.equals(mCurrentZnodeName)) {
        mCallback.onElectedToBeLeader();
        System.out.println("I'm the leader");
        return;
      } else {
        System.out.println("I'm not the leader");

        int predecessorIndex = Collections.binarySearch(children, mCurrentZnodeName) - 1;
        predecessorZnodeName = children.get(predecessorIndex);
        predecessorStat = mZooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZnodeName, this);
      }

      System.out.println("I'm not the leader, " + smallestChild + " is the leader");
    }

    mCallback.onWorker();

    System.out.println("Watching znode: " + predecessorZnodeName + "\n");
  }
}
