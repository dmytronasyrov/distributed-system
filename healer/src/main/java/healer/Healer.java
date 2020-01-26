package healer;

import org.apache.zookeeper.*;

import java.io.File;
import java.io.IOException;

public class Healer implements Watcher {

  // Constants

  private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
  private static final int SESSION_TIMEOUT = 3000;
  private static final String ZNODES_PATH = "/workers";

  // Variables

  private final int mNumOfWorkers;
  private final String mPathToProgram;
  private ZooKeeper mZooKeeper;

  // Main

  public static void main(String[] args) throws KeeperException, InterruptedException, IOException {
    if (args.length != 2) {
      System.out.println("Expecting params: <number of workers> <path to worker jar>");
      System.exit(1);
    }

    final int numOfWorkers = Integer.parseInt(args[0]);
    String pathToWorkerJar = args[1];

    final Healer healer = new Healer(numOfWorkers, pathToWorkerJar);
    healer.connectToZookeeper();
    healer.addWorkerZnode();
    healer.createWorkers();
    healer.watchWorkers();
    healer.run();
    healer.close();
  }

  // Constructors

  private Healer(int numOfWorkers, String pathToProgram) {
    mPathToProgram = pathToProgram;
    mNumOfWorkers = numOfWorkers;
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
    }
  }

  // Private

  private void connectToZookeeper() throws IOException {
    mZooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, watchedEvent -> {
    });
  }

  private void addWorkerZnode() throws KeeperException, InterruptedException {
    if (mZooKeeper.exists(ZNODES_PATH, false) == null)
      mZooKeeper.create(ZNODES_PATH, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
  }

  private void createWorkers() throws IOException {
    for (int i = 0; i < mNumOfWorkers; i++)
      createWorker();
  }

  private void createWorker() throws IOException {
    System.out.println("Creating new worker...");
    final File file = new File(mPathToProgram);
    final String runCommand = "java -jar " + file.getName();
    Runtime.getRuntime().exec(runCommand, null, file.getParentFile());
  }

  private void watchWorkers() {

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
