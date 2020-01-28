package client;

import client.networking.Aggregator;

import java.util.Arrays;
import java.util.List;

public class Application {

  // Constants

  private static final String WORKER_URL_1 = "http://localhost:8081/task";
  private static final String WORKER_URL_2 = "http://localhost:8082/task";

  // Main

  public static void main(String[] args) {
    final Aggregator aggregator = new Aggregator();

    final List<String> results = aggregator
      .sendTasksToWorkers(
        Arrays.asList(
          WORKER_URL_1,
          WORKER_URL_2
        ),
        Arrays.asList(
          "10,200",
          "123456789,1000000000000000,189273489173284712837"
        )
      );

    for (String result : results)
      System.out.println("Task result: " + result);
  }
}
