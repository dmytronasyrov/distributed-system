package client.networking;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class Client {

  // Variables

  private HttpClient mClient;

  // Constructors

  public Client() {
    mClient = HttpClient
      .newBuilder()
      .version(HttpClient.Version.HTTP_2)
      .build();
  }

  // Public

  public CompletableFuture<String> sendTask(String url, byte[] requestPayload) {
    final HttpRequest request = HttpRequest
      .newBuilder()
      .POST(HttpRequest.BodyPublishers.ofByteArray(requestPayload))
      .uri(URI.create(url))
      .header("X-Debug", "true")
      .build();

    return mClient
      .sendAsync(request, HttpResponse.BodyHandlers.ofString())
      .thenApply(response -> "Headers: " + response.headers().map().toString() + "\nResponse: " + response.body());
  }
}
