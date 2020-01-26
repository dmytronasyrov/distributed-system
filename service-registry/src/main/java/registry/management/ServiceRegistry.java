package registry.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher {

  // Constants

  private static final String REGISTRY_ZNODE = "/service_registry";

  // Variables

  private final ZooKeeper mZooKeeper;
  private String mCurrentZnode;
  private List<String> mAllServiceAddresses;

  // Constructors

  public ServiceRegistry(ZooKeeper zooKeeper) throws KeeperException, InterruptedException {
    mZooKeeper = zooKeeper;

    createServiceRegistryZnode();
  }

  // Public

  public void registerToCluster(String metadata) throws KeeperException, InterruptedException {
    mCurrentZnode = mZooKeeper.create(REGISTRY_ZNODE + "/n_", metadata.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    System.out.println("Registered to service registry");
  }

  public void registerForUpdates() throws KeeperException, InterruptedException {
    updateAddresses();
  }

  public synchronized List<String> getAllServiceAddresses() throws KeeperException, InterruptedException {
    if (mAllServiceAddresses == null)
      updateAddresses();

    return mAllServiceAddresses;
  }

  public void unregisterFromCluster() throws KeeperException, InterruptedException {
    if (mCurrentZnode != null && mZooKeeper.exists(mCurrentZnode, false) != null)
      mZooKeeper.delete(mCurrentZnode, -1);
  }

  // Overrides

  @Override
  public void process(WatchedEvent watchedEvent) {
    try {
      updateAddresses();
    } catch (KeeperException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  // Private

  private void createServiceRegistryZnode() throws KeeperException, InterruptedException {
    if (mZooKeeper.exists(REGISTRY_ZNODE, false) == null)
      mZooKeeper.create(REGISTRY_ZNODE, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
  }

  private void updateAddresses() throws KeeperException, InterruptedException {
    final List<String> workerZnodes = mZooKeeper.getChildren(REGISTRY_ZNODE, this);
    final ArrayList<String> addresses = new ArrayList<>(workerZnodes.size());

    for (String workerZnode : workerZnodes) {
      final String workerZnodeFullPath = REGISTRY_ZNODE + "/" + workerZnode;
      final Stat stat = mZooKeeper.exists(workerZnodeFullPath, false);

      if (stat == null)
        continue;

      final byte[] addressBytes = mZooKeeper.getData(workerZnodeFullPath, false, stat);
      final String address = new String(addressBytes);
      addresses.add(address);
    }

    mAllServiceAddresses = Collections.unmodifiableList(addresses);
    System.out.println("The cluster addresses are: " + mAllServiceAddresses);
  }
}