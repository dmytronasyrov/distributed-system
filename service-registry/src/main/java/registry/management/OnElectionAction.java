package registry.management;

import org.apache.zookeeper.KeeperException;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class OnElectionAction implements OnElectionCallback {

  // Variables

  private final ServiceRegistry mServiceRegistry;
  private final int mPort;

  // Constructors


  public OnElectionAction(ServiceRegistry serviceRegistry, int port) {
    mServiceRegistry = serviceRegistry;
    mPort = port;
  }

  // Overrides

  @Override
  public void onElectedToBeLeader() {
    try {
      mServiceRegistry.unregisterFromCluster();
      mServiceRegistry.registerForUpdates();
    } catch (KeeperException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onWorker() {
    try {
      final String server = String.format("http://%s:%d", InetAddress.getLocalHost().getCanonicalHostName(), mPort);
      mServiceRegistry.registerToCluster(server);
    } catch (KeeperException | InterruptedException | UnknownHostException e) {
      e.printStackTrace();
    }
  }
}
