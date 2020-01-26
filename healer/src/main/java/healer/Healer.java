package healer;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Healer implements Watcher {

  // Constants

  private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
  private static final int SESSION_TIMEOUT = 3000;
  private static final String ZNODES_PATH = "/workers";

  // Variables

  private final int mNumOfWorkers;
  private final String mPathToProgram;
  private final ZooKeeper mZooKeeper;

  // Main

  public static void main(String[] args) throws KeeperException, InterruptedException, IOException {
    if (args.length != 2) {
      System.out.println("Expecting params: <number of workers> <path to worker jar>");
      System.exit(1);
    }

    final int numOfWorkers = Integer.parseInt(args[0]);
    String pathToWorkerJar = args[1];

    final Healer healer = new Healer(numOfWorkers, pathToWorkerJar);
    healer.watchWorkers();
    healer.autoHeal();
    healer.run();
    healer.close();
  }

  // Constructors

  private Healer(int numOfWorkers, String pathToProgram) throws IOException {
    mPathToProgram = pathToProgram;
    mNumOfWorkers = numOfWorkers;
    mZooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, watchedEvent -> {});
  }

  // Overrides

  // Private

  @Override
  public void process(WatchedEvent watchedEvent) {
    switch (watchedEvent.getType()) {
      case None:
        onZooConnectionEvent(watchedEvent);
        break;

      case NodeCreated:
        System.out.println("New Znode created: " + watchedEvent.getPath());
        break;

      case NodeDeleted:
        System.out.println("Znode deleted: " + watchedEvent.getPath());
        break;

      case NodeChildrenChanged:
        System.out.println("Znode's children changed: " + watchedEvent.getPath());
        autoHeal();
        break;

      case NodeDataChanged:
        System.out.println("Znode's data changed: " + watchedEvent.getPath());
        break;

      default:
        break;
    }
  }

  // Private

  private void watchWorkers() throws KeeperException, InterruptedException {
    System.out.println("Starting to watch workers...");
    final Stat stat = mZooKeeper.exists(ZNODES_PATH, this);

    if (stat == null)
      mZooKeeper.create(ZNODES_PATH, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

    final byte[] data = mZooKeeper.getData(ZNODES_PATH, this, stat);
    final List<String> children = mZooKeeper.getChildren(ZNODES_PATH, this);
    System.out.println("Data: " + new String(data) + ", children: " + children);
  }

  private void autoHeal() {
    try {
      final List<String> children = mZooKeeper.getChildren(ZNODES_PATH, this);
      System.out.println("Children: " + children);

      if (children.size() < mNumOfWorkers)
        createWorker();
    } catch (KeeperException | InterruptedException | IOException e) {
      e.printStackTrace();
    }
  }

  private void createWorker() throws IOException {
    System.out.println("Creating new worker...");
    final File file = new File(mPathToProgram);
    final String runCommand = "java -jar " + file.getName();
    Runtime.getRuntime().exec(runCommand, null, file.getParentFile());
    System.out.println("Worker created");
  }

  private void run() throws InterruptedException {
    synchronized (mZooKeeper) {
      mZooKeeper.wait();
    }
  }

  private void close() throws InterruptedException {
    mZooKeeper.close();
  }

  private void onZooConnectionEvent(WatchedEvent watchedEvent) {
    if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
      System.out.println("Successfully connected to Zookeeper");
    } else {
      synchronized (mZooKeeper) {
        System.out.println("Disconnected from Zookeeper event");
        mZooKeeper.notifyAll();
      }
    }
  }
}
