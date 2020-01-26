package server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.Executors;

public class Server {

  // Constants

  private static final String TASK_ENDPOINT = "/task";
  private static final String STATUS_ENDPOINT = "/status";

  // Variables

  private final int mPort;
  private HttpServer mServer;

  // Main

  public static void main(String[] args) {
    int serverPort = 8080;

    if (args.length == 1)
      serverPort = Integer.parseInt(args[0]);

    final Server server = new Server(serverPort);
    server.startServer();

    System.out.println("Server is listening on port " + serverPort);
  }

  // Constructors

  private Server(int port) {
    mPort = port;
  }

  // Private

  private void startServer() {
    try {
      mServer = HttpServer.create(new InetSocketAddress(mPort), 0);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    final HttpContext statusContext = mServer.createContext(STATUS_ENDPOINT);
    statusContext.setHandler(this::handleStatusRequest);

    final HttpContext taskContext = mServer.createContext(TASK_ENDPOINT);
    taskContext.setHandler(this::handleTaskRequest);

    mServer.setExecutor(Executors.newFixedThreadPool(8));
    mServer.start();
  }

  private void handleStatusRequest(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equalsIgnoreCase("get"))
      exchange.close();
    else
      sendResponse("Server is alive".getBytes(), exchange);
  }

  private void handleTaskRequest(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestMethod().equalsIgnoreCase("post"))
      exchange.close();

    final Headers requestHeaders = exchange.getRequestHeaders();

    if (requestHeaders.containsKey("X-Test") && requestHeaders.get("X-Test").get(0).equalsIgnoreCase("true")) {
      sendResponse("123\n".getBytes(), exchange);
      return;
    }

    boolean isDebug = false;

    if (requestHeaders.containsKey("X-Debug") && requestHeaders.get("X-Debug").get(0).equalsIgnoreCase("true"))
      isDebug = true;

    final long startTime = System.nanoTime();

    final byte[] request = exchange.getRequestBody().readAllBytes();
    final byte[] response = calcResponse(request);

    final long finishTime = System.nanoTime();

    if (isDebug) {
      final String debugMsg = String.format("Operation took %d ns\n", finishTime - startTime);
      exchange.getResponseHeaders().put("X-Debug-Info", Collections.singletonList(debugMsg));
    }

    sendResponse(response, exchange);
  }

  private void sendResponse(byte[] response, HttpExchange exchange) throws IOException {
    exchange.sendResponseHeaders(200, response.length);

    final OutputStream outputStream = exchange.getResponseBody();
    outputStream.write(response);
    outputStream.flush();
    outputStream.close();
  }

  private byte[] calcResponse(byte[] request) {
    final String body = new String(request);
    final String[] numbers = body.split(",");
    BigInteger result = BigInteger.ONE;

    for (String number : numbers) {
      final BigInteger bigNumber = new BigInteger(number);
      result = result.multiply(bigNumber);
    }

    return String.format("Result of mult is %s\n", result).getBytes();
  }
}
