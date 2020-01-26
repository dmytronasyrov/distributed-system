package registry;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import registry.management.LeaderElection;
import registry.management.OnElectionAction;
import registry.management.ServiceRegistry;

import java.io.IOException;

public class Application implements Watcher {

  // Constants

  private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
  private static final int SESSION_TIMEOUT = 3000;
  private static final String ELECTION_NAMESPACE = "/election";
  private static final int DEFAULT_PORT = 8080;

  // Variables

  private final ZooKeeper mZooKeeper;

  // Main

  public static void main(String[] args) throws InterruptedException, IOException, KeeperException {
    int port = (args.length == 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT);

    final Application application = new Application();

    final ZooKeeper zooKeeper = application.connectToZookeeper();
    final ServiceRegistry serviceRegistry = new ServiceRegistry(zooKeeper);
    final OnElectionAction onElectionAction = new OnElectionAction(serviceRegistry, port);

    final LeaderElection leaderElection = new LeaderElection(zooKeeper, onElectionAction);
    leaderElection.volunteerForLeadership();
    leaderElection.reElectLeader();

    application.run();
    application.close();

    System.out.println("Disconnected from Zookeeper");
  }

  // Constructors

  private Application() throws IOException {
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
