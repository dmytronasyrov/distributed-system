package worker;

import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.Random;

public class Worker {

  // Constants

  private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
  private static final int SESSION_TIMEOUT = 3000;
  private static final String ZNODES_PATH = "/workers";
  private static final float CHANGE_TO_FAIL = 0.1f;

  // Variables

  private ZooKeeper mZooKeeper;
  private final Random mRandom = new Random();

  // Main

  public static void main(String[] args) throws IOException {
    final Worker worker = new Worker();
    worker.connectToZookeeper();
    worker.work();
  }

  // Private

  private void connectToZookeeper() throws IOException {
    mZooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, watchedEvent -> {
    });
  }

  private void work() {

  }
}
