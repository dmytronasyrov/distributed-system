package client.networking;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Aggregator {

  // Variables

  private Client mClient;

  // Constructors

  public Aggregator() {
    mClient = new Client();
  }

  // Public

  public List<String> sendTasksToWorkers(List<String> workersAddresses, List<String> tasks) {
    CompletableFuture<String>[] futures = new CompletableFuture[workersAddresses.size()];

    for (int i = 0; i < workersAddresses.size(); i++) {
      final String workerAddress = workersAddresses.get(i);
      final String task = tasks.get(i);
      final byte[] payload = task.getBytes();
      futures[i] = mClient.sendTask(workerAddress, payload);
    }

    return Stream.of(futures)
      .map(CompletableFuture::join)
      .collect(Collectors.toList());
  }
}
