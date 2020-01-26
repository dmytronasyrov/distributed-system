package leader;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {

  // Constants

  private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
  private static final int SESSION_TIMEOUT = 3000;
  public static final String ELECTION_NAMESPACE = "/election";

  // Variables

  private ZooKeeper mZooKeeper;
  private String mCurrentZnodeName;

  // Main

  public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
    final LeaderElection leaderElection = new LeaderElection();

    leaderElection.connectToZookeeper();
    leaderElection.volunteerForLeadership();
    leaderElection.reElectLeader();
    leaderElection.run();
    leaderElection.close();

    System.out.println("Disconnected from Zookeeper");
  }

  // Overrides

  @Override
  public void process(WatchedEvent watchedEvent) {
    switch (watchedEvent.getType()) {
      case None:
        if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
          System.out.println("Successfully connected to Zookeeper");
        } else {
          synchronized (mZooKeeper) {
            System.out.println("Disconnected from Zookeeper event");

            mZooKeeper.notifyAll();
          }
        }
        break;

      case NodeDeleted:
        try {
          reElectLeader();
        } catch (KeeperException | InterruptedException e) {
          e.printStackTrace();
        }
    }
  }

  // Private

  private void connectToZookeeper() throws IOException {
    mZooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
  }

  private void volunteerForLeadership() throws KeeperException, InterruptedException {
    if (mZooKeeper.exists(ELECTION_NAMESPACE, false) == null) {
      final String electionZnode = mZooKeeper.create(ELECTION_NAMESPACE, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      System.out.println("Election Znode created: " + electionZnode);
    }

    final String znodePrefix = ELECTION_NAMESPACE + "/c_";
    final String znoodeFullPath = mZooKeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    mCurrentZnodeName = znoodeFullPath.replace(ELECTION_NAMESPACE + "/", "");

    System.out.println("Znode name: " + znoodeFullPath);
  }

  private void reElectLeader() throws KeeperException, InterruptedException {
    Stat predecessorStat = null;
    String predecessorZnodeName = "";

    while (predecessorStat == null) {
      final List<String> children = mZooKeeper.getChildren(ELECTION_NAMESPACE, false);
      Collections.sort(children);
      final String smallestChild = children.get(0);

      if (smallestChild.equals(mCurrentZnodeName)) {
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

    System.out.println("Watching znode: " + predecessorZnodeName + "\n");
  }

  private void run() throws InterruptedException {
    synchronized (mZooKeeper) {
      mZooKeeper.wait();
    }
  }

  private void close() throws InterruptedException {
    mZooKeeper.close();
  }
}
