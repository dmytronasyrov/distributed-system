package worker;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;

public class Worker {

  // Constants

  private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
  private static final int SESSION_TIMEOUT = 3000;
  private static final String ZNODES_PATH = "/workers";
  private static final float CHANCE_TO_FAIL = 0.1f;

  // Variables

  private ZooKeeper mZooKeeper;
  private final Random mRandom = new Random();

  // Main

  public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
    final Worker worker = new Worker();
    worker.connectToZookeeper();
    worker.addWorkerZnode();
    worker.work();
  }

  // Private

  private void connectToZookeeper() throws IOException {
    System.out.println("New worker is connecting to Zoo...");
    mZooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, watchedEvent -> {
    });
  }

  private void work() throws KeeperException, InterruptedException {
    while (true) {
      System.out.println("Working...");

      LockSupport.parkNanos(1000000000);

      if (mRandom.nextFloat() < CHANCE_TO_FAIL) {
        System.out.println("Worker is failing...");
        throw new RuntimeException("Shit, I'm failing!");
      }
    }
  }

  private void addWorkerZnode() throws KeeperException, InterruptedException {
    System.out.println("Adding worker's znode...");
    mZooKeeper.create(ZNODES_PATH + "/worker_", new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
  }
}
