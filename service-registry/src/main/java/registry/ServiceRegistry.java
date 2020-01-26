package registry;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import registry.management.LeaderElection;

import java.io.IOException;

public class ServiceRegistry implements Watcher {

  // Constants

  private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
  private static final int SESSION_TIMEOUT = 3000;
  private static final String ELECTION_NAMESPACE = "/election";

  // Variables

  private final ZooKeeper mZooKeeper;

  // Main

  public static void main(String[] args) throws InterruptedException, IOException, KeeperException {
    final ServiceRegistry serviceRegistry = new ServiceRegistry();
    final ZooKeeper zooKeeper = serviceRegistry.connectToZookeeper();

    final LeaderElection leaderElection = new LeaderElection(zooKeeper);
    leaderElection.volunteerForLeadership();
    leaderElection.reElectLeader();

    serviceRegistry.run();
    serviceRegistry.close();

    System.out.println("Disconnected from Zookeeper");
  }

  // Constructors

  private ServiceRegistry() throws IOException {
    mZooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
  }

  // Override

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
    }
  }

  // Private

  private ZooKeeper connectToZookeeper() throws KeeperException, InterruptedException {
    final Stat stat = mZooKeeper.exists(ELECTION_NAMESPACE, this);

    if (stat == null)
      mZooKeeper.create(ELECTION_NAMESPACE, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

    return mZooKeeper;
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
